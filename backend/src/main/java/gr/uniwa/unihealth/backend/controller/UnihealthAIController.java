package gr.uniwa.unihealth.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.controller.request.ChatRequest;
import gr.uniwa.unihealth.backend.controller.request.ConfirmActionRequest;
import gr.uniwa.unihealth.backend.controller.response.ChatResponse;
import gr.uniwa.unihealth.backend.service.UnihealthAIService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.ai.AiTurnProgress;
import gr.uniwa.unihealth.backend.service.ai.action.ActionExecutionService;
import gr.uniwa.unihealth.backend.service.ai.conversation.AiConversationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("ai")
@RequiredArgsConstructor
public class UnihealthAIController {

  private final UnihealthAIService aiService;
  private final ActionExecutionService actionExecutionService;
  private final TenantContext tenantContext;
  private final AiConversationService conversationService;
  private final UserReaderService userReaderService;

  /** Its own mapper: this project runs Jackson 3 for HTTP, and these lines are hand-written. */
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Operation(summary = "Ask the assistant",
      description = "Answers one student health question in a single response. Prefer "
          + "`/ai/chat/stream` in the UI: a turn can take ten seconds on modest hardware.")
  @PostMapping("/chat")
  public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
    // @Valid, not merely the constraints on the record: without it the @NotBlank and @Size on
    // ChatRequest are inert annotations, which is what let an unbounded body through before.
    return record(request, aiService.answer(request.message(), AiTurnProgress.NONE));
  }

  /**
   * Files a completed turn under a conversation, and returns it carrying that conversation's id.
   *
   * <p><b>The one condition here is the whole storage policy.</b> A {@code safetyKey} means the
   * turn never reached the model - either {@code CrisisGuard} screened it, or the corpus was still
   * being embedded - and such a turn is answered and then forgotten. On a brand-new chat that
   * means no conversation row is created at all, so a student's «θέλω να πεθάνω» leaves nothing
   * behind, consistent with the guard already logging only which authored phrase matched.
   *
   * <p>Persistence lives in the controller rather than in the pipeline so that the pipeline stays
   * unaware storage exists, and so the id can be decided after it is known the turn is storable.
   */
  private ChatResponse record(ChatRequest request, ChatResponse response) {
    if (response.safetyKey() != null) {
      return response;
    }

    try {
      String conversationId = conversationService.recordTurn(
          userReaderService.findLoggedInUser().getId(), request.conversationId(),
          request.message(), response);

      return response.withConversationId(conversationId);
    } catch (Exception e) {
      // The answer already exists and the student is waiting for it. Losing the transcript of one
      // turn is a far smaller harm than throwing away a reply that took ten seconds to produce.
      log.warn("Could not record the chat turn: {}", e.getMessage(), e);
      return response;
    }
  }

  /**
   * The same turn, reporting each stage as it starts.
   *
   * <p>Newline-delimited JSON rather than Server-Sent Events, for one practical reason: SSE in a
   * browser means {@code EventSource}, which cannot send an {@code Authorization} header, and every
   * endpoint here is behind a Keycloak bearer token. NDJSON over an ordinary POST keeps the
   * existing interceptors and needs no token in a query string.
   *
   * <p>Lines are {@code {"type":"stage","key":...}} until the last, which is
   * {@code {"type":"answer", ...}} carrying exactly the {@link ChatResponse} the plain endpoint
   * would have returned.
   *
   * <p><b>The context capture below is load-bearing.</b> Spring MVC runs a
   * {@link StreamingResponseBody} on a different thread from the one that handled the request, and
   * neither of the two things this application needs travels with it: the tenant lives in a
   * {@code ScopedValue}, which is not inherited, and the authentication lives in a
   * {@code ThreadLocal}. Without re-binding both, every database read in the pipeline would hit
   * the wrong tenant - or no tenant at all - and every call to
   * {@code AuthenticationContext.getCurrentUsername()} would return "system". Neither failure
   * announces itself.
   */
  @Operation(summary = "Ask the assistant, streaming progress",
      description = "Newline-delimited JSON. Emits a `stage` line as each stage begins, then a "
          + "final `answer` line identical to the response from `/ai/chat`.")
  @PostMapping(value = "/chat/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
  public ResponseEntity<StreamingResponseBody> chatStream(@Valid @RequestBody ChatRequest request) {
    String tenant = tenantContext.getCurrentTenant();
    SecurityContext securityContext = SecurityContextHolder.getContext();

    StreamingResponseBody body = out -> {
      SecurityContextHolder.setContext(securityContext);
      try {
        // inVirtualThread = false, so this runs inline on the streaming thread with the tenant
        // ScopedValue bound around it, rather than hopping to yet another thread.
        tenantContext.runAs(tenant, () -> runTurn(request, out), false);
      } finally {
        SecurityContextHolder.clearContext();
      }
    };

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_NDJSON)
        .cacheControl(CacheControl.noStore())
        // Told to the proxy by the response, which is how X-Accel-Buffering works - it is not a
        // request header. nginx already has proxy_buffering off for this path; this covers any
        // other intermediary, and keeps the requirement visible from the code that depends on it.
        .header("X-Accel-Buffering", "no")
        .body(body);
  }

  private void runTurn(ChatRequest request, OutputStream out) {
    try {
      ChatResponse response = aiService.answer(request.message(), stage -> write(out,
          Map.of("type", "stage", "key", stage.messageKey())));

      // Filed before the answer line goes out, so the id travels with it and a client that just
      // started a chat learns which one it is. Nothing is emitted earlier than this: doing so
      // would mean creating the conversation before knowing whether the turn is storable.
      write(out, Map.of("type", "answer", "payload", record(request, response)));

    } catch (Exception e) {
      log.warn("Streamed chat turn failed: {}", e.getMessage(), e);

      // A lexicon key rather than the exception text: the student is owed a sentence in their own
      // language, and an internal message is not one.
      write(out, Map.of("type", "error", "key", "ai.chat.error"));
    }
  }

  /**
   * Writes one line and flushes it.
   *
   * <p>The flush is the entire point - without it the servlet container buffers and the student
   * sees every stage arrive at once, at the end, which is the same as seeing none of them.
   *
   * <p>Failures are swallowed. A closed connection means the student navigated away, which is
   * normal and must not become an exception inside a pipeline that is mid-turn.
   */
  private void write(OutputStream out, Map<String, Object> line) {
    String json;

    // Serialised before the write, and reported separately, because JsonProcessingException
    // extends IOException: folded into one catch, a real serialisation bug would be logged for
    // years as "the student closed the tab".
    try {
      json = objectMapper.writeValueAsString(line);
    } catch (Exception e) {
      log.warn("Could not serialise a progress line: {}", e.getMessage(), e);
      return;
    }

    try {
      out.write(json.getBytes(StandardCharsets.UTF_8));
      out.write('\n');
      out.flush();
    } catch (IOException e) {
      log.debug("Client disconnected mid-turn: {}", e.getMessage());
    }
  }

  /**
   * The only endpoint in this feature that writes anything.
   *
   * <p>Separate from {@code /chat} on purpose. A change is applied because a person clicked, never
   * because a model emitted a sentence: the assistant does not hold the {@code actionId} and has no
   * route to this method. Self-service, so no administrator permission is required - the settings
   * being changed are the caller's own, and the service resolves whose they are from the security
   * context rather than from this request.
   */
  @Operation(summary = "Answer a confirmation card",
      description = "Applies or discards a change the assistant proposed. Single-use: an actionId "
          + "already answered, expired, or belonging to another user is refused.")
  @PostMapping("/chat/confirm")
  public ActionExecutionService.Outcome confirm(@Valid @RequestBody ConfirmActionRequest request) {
    return actionExecutionService.resolve(request.actionId(), request.approved());
  }
}

package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.controller.request.ChatRequest;
import gr.uniwa.unihealth.backend.controller.response.ChatResponse;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.service.UnihealthAIService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.ai.AiTurnProgress;
import gr.uniwa.unihealth.backend.service.ai.action.ActionExecutionService;
import gr.uniwa.unihealth.backend.service.ai.conversation.AiConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The storage rule, asserted where it actually lives.
 *
 * <p><b>A turn that never reached the model is never persisted.</b> One condition in the
 * controller covers both halves of that: a message screened by {@code CrisisGuard}, and the reply
 * sent while the corpus is still being embedded. Both come back carrying a {@code safetyKey}.
 *
 * <p>It is worth a test of its own because the failure is invisible in use - a student's
 * «θέλω να πεθάνω» would be answered correctly, with the helpline, and quietly filed under a
 * conversation titled with those words. Nothing on screen would say so.
 *
 * @author omaro
 */
@ExtendWith(MockitoExtension.class)
class UnihealthAIControllerTest {

  @Mock
  private UnihealthAIService aiService;

  @Mock
  private ActionExecutionService actionExecutionService;

  @Mock
  private TenantContext tenantContext;

  @Mock
  private AiConversationService conversationService;

  @Mock
  private UserReaderService userReaderService;

  private UnihealthAIController controller;

  @BeforeEach
  void setUp() {
    controller = new UnihealthAIController(aiService, actionExecutionService, tenantContext,
        conversationService, userReaderService);
  }

  private void loggedInAs(String userId) {
    UserDTO user = new UserDTO();

    user.setId(userId);
    when(userReaderService.findLoggedInUser()).thenReturn(user);
  }

  private static ChatResponse answered() {
    return new ChatResponse("Πιες νερό.", null, List.of(), true, null, null);
  }

  @Test
  @DisplayName("a screened first message creates no conversation at all")
  void screenedTurnsAreNeverStored() {
    when(aiService.answer(any(), any(AiTurnProgress.class)))
        .thenReturn(ChatResponse.safety("ai.safety.selfHarm"));

    ChatResponse response = controller.chat(new ChatRequest("θέλω να πεθάνω", null));

    // Not "an empty conversation" and not "a conversation with one message" - none at all. The
    // student still gets the helpline; the transcript is simply never written.
    verifyNoInteractions(conversationService);
    assertThat(response.safetyKey()).isEqualTo("ai.safety.selfHarm");
    assertThat(response.conversationId()).isNull();
  }

  @Test
  @DisplayName("the same rule holds inside an existing conversation")
  void screenedTurnsInsideAConversationAreNotStoredEither() {
    // The condition is on the response, not on whether a conversation already exists, so a crisis
    // phrase typed into a chat that has been going for a week is still written nowhere.
    when(aiService.answer(any(), any(AiTurnProgress.class)))
        .thenReturn(ChatResponse.safety("ai.chat.libraryPreparing"));

    ChatResponse response = controller.chat(new ChatRequest("Έχω πονοκέφαλο", "c1"));

    verifyNoInteractions(conversationService);
    assertThat(response.conversationId()).isNull();
  }

  @Test
  @DisplayName("an answered turn is filed, and comes back carrying its conversation id")
  void answeredTurnsAreStored() {
    loggedInAs("user-1");
    when(aiService.answer(any(), any(AiTurnProgress.class))).thenReturn(answered());
    when(conversationService.recordTurn(eq("user-1"), isNull(), eq("Έχω πονοκέφαλο"), any()))
        .thenReturn("new-conversation");

    ChatResponse response = controller.chat(new ChatRequest("Έχω πονοκέφαλο", null));

    // The id rides back on the response, which is how a client that just started a chat learns
    // which one it is - it never chooses the id itself.
    assertThat(response.conversationId()).isEqualTo("new-conversation");
    assertThat(response.reply()).isEqualTo("Πιες νερό.");
    verify(conversationService).recordTurn(eq("user-1"), isNull(), eq("Έχω πονοκέφαλο"), any());
  }

  @Test
  @DisplayName("losing the transcript does not lose the answer")
  void aFailedRecordStillReturnsTheReply() {
    // The answer took ten seconds on this hardware and the student is waiting for it. Failing the
    // request because the history row could not be written would be the wrong trade by a mile.
    loggedInAs("user-1");
    when(aiService.answer(any(), any(AiTurnProgress.class))).thenReturn(answered());
    when(conversationService.recordTurn(any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("database is having a day"));

    ChatResponse response = controller.chat(new ChatRequest("Έχω πονοκέφαλο", null));

    assertThat(response.reply()).isEqualTo("Πιες νερό.");
    assertThat(response.conversationId()).isNull();
  }
}

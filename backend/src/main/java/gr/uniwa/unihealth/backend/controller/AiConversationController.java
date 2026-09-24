package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.dto.ai.AiConversationDTO;
import gr.uniwa.unihealth.backend.dto.ai.AiConversationDetailDTO;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.ai.conversation.AiConversationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provides access to the logged-in user's AI conversations.
 * <p>All operations are scoped to the authenticated user.
 *
 * @author omaro
 **/
@RestController
@RequestMapping("ai-conversation")
@RequiredArgsConstructor
public class AiConversationController {

  /** Maximum number of conversations returned at once. */
  private static final int MAX_LIMIT = 50;

  private final AiConversationService service;
  private final UserReaderService userReaderService;

  @GetMapping("_me")
  @Operation(summary = "Lists the caller's own conversations, most recently used first")
  public List<AiConversationDTO> findMine(
      @RequestParam(defaultValue = "30") int limit) {
    return service.findMine(currentUserId(), Math.clamp(limit, 1, MAX_LIMIT));
  }

  @GetMapping("_me/{id}")
  @Operation(summary = "Opens one of the caller's own conversations",
      description = "Answers 404 for a conversation belonging to somebody else: a 403 would "
          + "confirm that the id exists.")
  public AiConversationDetailDTO findMine(@PathVariable String id) {
    return service.findMine(currentUserId(), id);
  }

  @DeleteMapping("_me/{id}")
  @Operation(summary = "Deletes one of the caller's own conversations",
      description = "The messages go with it.")
  public void deleteMine(@PathVariable String id) {
    service.deleteMine(currentUserId(), id);
  }

  @DeleteMapping("_me")
  @Operation(summary = "Deletes every conversation the caller has")
  public void deleteAllMine() {
    service.deleteAllMine(currentUserId());
  }

  private String currentUserId() {
    return userReaderService.findLoggedInUser().getId();
  }
}

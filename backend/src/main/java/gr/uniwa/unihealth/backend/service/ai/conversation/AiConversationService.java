package gr.uniwa.unihealth.backend.service.ai.conversation;

import gr.uniwa.unihealth.backend.controller.response.ChatResponse;
import gr.uniwa.unihealth.backend.dto.ai.AiConversationDTO;
import gr.uniwa.unihealth.backend.dto.ai.AiConversationDetailDTO;

import java.util.List;

/**
 * Manages a user's saved AI conversations.
 *
 * @author omaro
 */
public interface AiConversationService {

  /**
   * Saves a completed chat turn, creating a conversation when needed.
   *
   * @param userId owner of the conversation
   * @param conversationId existing conversation id, or {@code null} for a new one
   * @param question user's message
   * @param response assistant's response
   * @return the conversation id
   */
  String recordTurn(String userId, String conversationId, String question, ChatResponse response);

  /** Returns the user's most recent conversations. */
  List<AiConversationDTO> findMine(String userId, int limit);

  /** Returns a conversation with its messages. */
  AiConversationDetailDTO findMine(String userId, String conversationId);

  /** Deletes one of the user's conversations. */
  void deleteMine(String userId, String conversationId);

  /** Deletes all conversations belonging to the user. */
  void deleteAllMine(String userId);
}

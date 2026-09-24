package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.AiMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Messages are only ever reached through a conversation the caller has already been proven to own,
 * which is why these methods take a conversation id rather than a user id.
 *
 * <p>That proof is not optional: a caller must load the conversation through
 * {@link AiConversationRepository#findByIdAndUserId} first. Reading messages by conversation id
 * alone, without that step, would read somebody else's transcript.
 *
 * @author omaro
 */
@Repository
public interface AiMessageRepository extends BaseRepository<AiMessage> {

  List<AiMessage> findByConversationIdOrderBySequenceNoAsc(String conversationId);

  /** Gives the next message its position without loading the conversation's whole history. */
  int countByConversationId(String conversationId);
}

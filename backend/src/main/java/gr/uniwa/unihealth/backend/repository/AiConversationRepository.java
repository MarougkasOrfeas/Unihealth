package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.AiConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for user-owned AI conversations.
 * <p>All conversation lookups are scoped by user to prevent access to another user's chats.
 *
 * @author omaro */
@Repository
public interface AiConversationRepository extends BaseRepository<AiConversation> {

  Optional<AiConversation> findByIdAndUserId(String id, String userId);


  /** Returns the user's conversations, newest first. */
  List<AiConversation> findByUserIdOrderByLastMessageOnDesc(String userId, Pageable pageable);

  /** Deletes all conversations belonging to the user. */
  void deleteByUserId(String userId);
}

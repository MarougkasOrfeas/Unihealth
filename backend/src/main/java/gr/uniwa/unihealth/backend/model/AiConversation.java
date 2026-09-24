package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a saved AI conversation belonging to a user.
 * <p>Stores the information needed to display and organize conversations.
 * Messages are stored separately in {@code AiMessage}.
 *
 * @author omaro
 * */
@Getter
@Setter
@Entity
@Table(name = "t_ai_conversation")
public class AiConversation extends BaseUpdatableEntity {

  /** User who owns the conversation. */
  @Column(name = "user_id", updatable = false, nullable = false)
  private String userId;

  /** Title derived from the conversation's first question. */
  @Column(nullable = false, length = 200)
  private String title;

  /** Time of the most recent message, used to sort conversations. */
  @Column(name = "last_message_on", nullable = false)
  private LocalDateTime lastMessageOn;
}

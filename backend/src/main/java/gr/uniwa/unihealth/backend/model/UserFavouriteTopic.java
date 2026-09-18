package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A topic the user has marked as a favourite.
 *
 * Extends {@link BaseEntity} rather than {@link BaseUpdatableEntity} on purpose: a favourite is
 * created and deleted, never edited, so modification and version columns would be dead weight.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_user_favourite_topic",
    uniqueConstraints = @UniqueConstraint(name = "uq_user_favourite_topic",
        columnNames = {"user_id", "topic_id"}))
public class UserFavouriteTopic extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * The identifier of the favourited topic. Held as a plain string with no foreign key, because
   * topic content is not in the database yet — it is served from the frontend mock until real
   * content is ingested. The foreign key belongs in the same changeset that creates that table.
   */
  @Column(name = "topic_id", nullable = false, length = 64)
  private String topicId;
}

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

import java.time.LocalDateTime;

/**
 * Measured engagement with one profiling label, for one user.
 *
 * Extends {@link BaseUpdatableEntity} rather than {@link BaseEntity}: unlike a favourite, which is
 * only created and deleted, a metric accumulates over time and is written repeatedly.
 *
 * Deliberately an aggregate rather than an event log — one row per (user, label), upserted. An
 * event table would grow without bound and need aggregating on every read.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_user_label_metric",
    uniqueConstraints = @UniqueConstraint(name = "uq_user_label_metric",
        columnNames = {"user_id", "label_code"}))
public class UserLabelMetric extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * The profiling label code, e.g. {@code GOAL_SLEEP_BETTER}. No foreign key to
   * {@code t_label_field_mapping}: codes are produced by the label evaluator, and a mapping can be
   * deactivated while metrics recorded against it remain. Orphans are expected.
   */
  @Column(name = "label_code", nullable = false, length = 64)
  private String labelCode;

  /** Total seconds spent on content targeting this label. */
  @Column(name = "view_seconds", nullable = false)
  private long viewSeconds;

  /** How many times the user engaged with content targeting this label. */
  @Column(name = "interaction_count", nullable = false)
  private int interactionCount;

  @Column(name = "last_seen_on")
  private LocalDateTime lastSeenOn;
}

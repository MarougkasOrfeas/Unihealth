package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.enums.MedicalTestCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * An uploaded file. The bytes live in object storage under this row's id; this is the metadata.
 */
@Getter
@Setter
@Entity
@Table(name = "t_file")
public class FileEntity extends BaseUpdatableEntity {

  private String name;
  private String description;
  private boolean active;

  @Column(name = "content_type", updatable = false)
  private String contentType;

  @Column(name = "size", updatable = false)
  private long fileSize;

  /**
   * The owner, as a {@code t_user} id.
   *
   * <p>A plain column rather than a {@code @ManyToOne}: nothing ever renders the owner — it is
   * always the person asking — so an association would add a lazy proxy and an implicit join to
   * every list query in exchange for nothing. The foreign key is declared in changeset 00015.
   *
   * <p>{@code updatable = false} because a document never changes hands.
   */
  @Column(name = "user_id", updatable = false, nullable = false)
  private String userId;

  /** When the examination took place, which is rarely when the file was uploaded. */
  @Column(name = "exam_date")
  private LocalDate examDate;

  /**
   * The year of {@link #examDate}, derived rather than entered.
   *
   * <p>Exists purely so the list can offer a "filter by year" dropdown. Facet values are produced
   * and consumed as strings, so faceting the date column directly would offer every individual
   * examination date instead of a handful of years. Kept in step by {@link #deriveExamYear()} so it
   * cannot drift from the date it summarises.
   */
  @Column(name = "exam_year", length = 4)
  private String examYear;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private MedicalTestCategory category;

  /**
   * Derived in a lifecycle callback rather than in a setter or a service, so it holds however the
   * date was set — including a bulk update or a mapper writing the field directly.
   */
  @PrePersist
  @PreUpdate
  private void deriveExamYear() {
    examYear = examDate != null ? String.valueOf(examDate.getYear()) : null;
  }
}

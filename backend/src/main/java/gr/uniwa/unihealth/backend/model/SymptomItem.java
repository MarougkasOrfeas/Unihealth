package gr.uniwa.unihealth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_symptom_item")
public class SymptomItem extends BaseUpdatableEntity {

  private String title;
  private String slug;

  @Column(name = "starting_letter")
  private String startingLetter;

  private String brief;

  @Column(name = "overview_text")
  private String overviewText;

  @Column(name = "symptoms_text")
  private String symptomsText;

  @Column(name = "do_text")
  private String doText;

  @Column(name = "dont_text")
  private String dontText;

  @Column(name = "see_doctor_if_text")
  private String seeDoctorIfText;

  @Column(name = "treatment_text")
  private String treatmentText;

  @Column(name = "causes_text")
  private String causesText;

  /** NHS "ask for an urgent GP appointment or get advice from 111 now" content. */
  @Column(name = "urgent_text")
  private String urgentText;

  /** NHS "call 999 or go to A&amp;E now" content. Always shown, never ranked away. */
  @Column(name = "emergency_text")
  private String emergencyText;

  /** Alternative names for the symptom, comma separated, so search finds "heart pain". */
  private String synonyms;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id")
  private DataSource source;

  @Column(name = "source_url")
  private String sourceUrl;

  @Column(name = "source_last_reviewed")
  private LocalDateTime sourceLastReviewed;

  /** Whether this symptom has a causes table, and therefore an advanced search to offer. */
  @Column(name = "has_factors")
  private boolean hasFactors;

  @Column(name = "display_order")
  private Integer displayOrder;

  private boolean active;
}

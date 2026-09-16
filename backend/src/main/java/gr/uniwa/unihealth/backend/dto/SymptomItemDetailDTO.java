package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SymptomItemDetailDTO extends BaseUpdatableDTO {

  private String title;
  private String slug;
  private String startingLetter;
  private String brief;
  private String synonyms;

  private String overviewText;
  private String symptomsText;
  private String doText;
  private String dontText;
  private String seeDoctorIfText;
  /** Get help today. Shown above the rest, never ranked away. */
  private String urgentText;
  /** Call 999. Shown above the rest, never ranked away. */
  private String emergencyText;
  private String treatmentText;
  private String causesText;

  private boolean hasFactors;
  private DataSourceDTO source;
  private String sourceUrl;
  private LocalDateTime sourceLastReviewed;
  private boolean active;
}

package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SymptomItemDetailDTO extends BaseUpdatableDTO {

  private String title;
  private String slug;
  private String startingLetter;
  private String brief;

  private String overviewText;
  private String symptomsText;
  private String doText;
  private String dontText;
  private String seeDoctorIfText;
  private String treatmentText;
  private String causesText;
  private boolean active;
}

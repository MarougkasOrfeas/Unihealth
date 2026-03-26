package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionalHealthProfileDTO extends BaseUpdatableDTO {

  private String sleepQuality;
  private String studyLoad;
  private String smoking;
  private String coffee;
  private String screenTime;
  private String exercise;
  private String mealsPerDay;
  private String eatSnack;
  private String water;
  private String dietType;
  private Boolean medication;
  private String medicationDetails;
  private Boolean surgeryHistory;
  private String surgeryDetails;
  private String preferredContent;
  private String frequency;
  private String comments;
}

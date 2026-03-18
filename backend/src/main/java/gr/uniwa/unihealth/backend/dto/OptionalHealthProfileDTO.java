package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionalHealthProfileDTO extends BaseUpdatableDTO {

  // Daily Wellbeing
  private String sleepQuality;
  private String stressLevel;
  private String studyLoad;

  // Healthy Habits
  private String activityLevel;
  private Integer exerciseFrequencyPerWeek;
  private String dietType;
  private String mealRegularity;
  private String hydrationLevel;

  // Physical Condition
  private String fitnessLevel;
  private Boolean hasPhysicalLimitations;
  private String physicalLimitationsDetails;

  // Preferences
  private String preferredRoutineTime;
  private String preferredContentType;
  private String wellnessFocus;
}

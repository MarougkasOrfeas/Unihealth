package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.enums.optionalFormFieldEnum.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_optional_health_profile")
public class OptionalHealthProfile extends BaseUpdatableEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "health_profile_id", nullable = false, unique = true)
  private HealthProfile healthProfile;

  @Enumerated(EnumType.STRING)
  @Column(name = "sleep_quality")
  private SleepQuality sleepQuality;

  @Enumerated(EnumType.STRING)
  @Column(name = "study_load")
  private StudyLoad studyLoad;

  @Enumerated(EnumType.STRING)
  private SmokingHabit smoking;

  @Enumerated(EnumType.STRING)
  private CoffeeConsumption coffee;

  @Enumerated(EnumType.STRING)
  @Column(name = "screen_time")
  private ScreenTime screenTime;

  @Enumerated(EnumType.STRING)
  private ExerciseFrequency exercise;

  @Enumerated(EnumType.STRING)
  @Column(name = "meals_per_day")
  private MealsPerDay mealsPerDay;

  @Enumerated(EnumType.STRING)
  @Column(name = "eat_snack")
  private SnackFrequency eatSnack;

  @Enumerated(EnumType.STRING)
  private WaterIntake water;

  @Enumerated(EnumType.STRING)
  @Column(name = "diet_type")
  private DietType dietType;

  private Boolean medication;

  @Column(name = "medication_details")
  private String medicationDetails;

  @Column(name = "surgery_history")
  private Boolean surgeryHistory;

  @Column(name = "surgery_details")
  private String surgeryDetails;

  @Enumerated(EnumType.STRING)
  @Column(name = "preferred_content")
  private PreferredContentType preferredContent;

  @Enumerated(EnumType.STRING)
  private ContentFrequency frequency;

  private String comments;
}

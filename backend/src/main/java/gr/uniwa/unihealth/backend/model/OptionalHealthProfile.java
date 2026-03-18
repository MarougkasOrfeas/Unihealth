package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.enums.optionalFormFieldEnum.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_optional_health_profile")
public class OptionalHealthProfile extends BaseUpdatableEntity{

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "health_profile_id", nullable = false, unique = true)
  private HealthProfile healthProfile;

  // Daily Wellbeing
  @Enumerated(EnumType.STRING)
  @Column(name = "sleep_quality")
  private SleepQuality sleepQuality;

  @Enumerated(EnumType.STRING)
  @Column(name = "stress_level")
  private StressLevel stressLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "study_load")
  private StudyLoad studyLoad;

  // Healthy Habits
  @Enumerated(EnumType.STRING)
  @Column(name = "activity_level")
  private ActivityLevel activityLevel;

  @Column(name = "exercise_frequency_per_week")
  private Integer exerciseFrequencyPerWeek;

  @Enumerated(EnumType.STRING)
  @Column(name = "diet_type")
  private DietType dietType;

  @Enumerated(EnumType.STRING)
  @Column(name = "meal_regularity")
  private MealRegularity mealRegularity;

  @Enumerated(EnumType.STRING)
  @Column(name = "hydration_level")
  private HydrationLevel hydrationLevel;

  // Physical Condition
  @Enumerated(EnumType.STRING)
  @Column(name = "fitness_level")
  private FitnessLevel fitnessLevel;

  @Column(name = "has_physical_limitations")
  private Boolean hasPhysicalLimitations;

  @Column(name = "physical_limitations_details")
  private String physicalLimitationsDetails;

  // Preferences
  @Enumerated(EnumType.STRING)
  @Column(name = "preferred_routine_time")
  private PreferredRoutineTime preferredRoutineTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "preferred_content_type")
  private PreferredContentType preferredContentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "wellness_focus")
  private WellnessFocus wellnessFocus;
}

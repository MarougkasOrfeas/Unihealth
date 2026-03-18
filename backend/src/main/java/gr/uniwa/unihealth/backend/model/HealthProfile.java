package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.enums.Gender;
import gr.uniwa.unihealth.backend.model.enums.PrimaryGoal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "t_health_profile")
public class HealthProfile extends BaseUpdatableEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  @Column(name = "height_cm")
  private Integer heightCm;

  @Column(name = "weight_kg")
  private BigDecimal weightKg;

  @Column(name = "has_food_allergies")
  private boolean hasFoodAllergies;

  @Column(name = "food_allergies_details")
  private String foodAllergiesDetails;

  @Column(name = "has_chronic_conditions")
  private boolean hasChronicConditions;

  @Column(name = "chronic_conditions_details")
  private String chronicConditionsDetails;

  @Enumerated(EnumType.STRING)
  @Column(name = "primary_goal")
  private PrimaryGoal primaryGoal;

  @OneToOne(mappedBy = "healthProfile")
  private OptionalHealthProfile optionalProfile;
}

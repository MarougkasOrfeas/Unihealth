package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.Gender;
import gr.uniwa.unihealth.backend.model.enums.PrimaryGoal;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class HealthProfileDTO extends BaseUpdatableDTO {

  private LocalDate dateOfBirth;
  private Gender gender;
  private Integer heightCm;
  private BigDecimal weightKg;
  private boolean hasFoodAllergies;
  private String foodAllergiesDetails;
  private boolean hasChronicConditions;
  private String chronicConditionsDetails;
  private PrimaryGoal primaryGoal;
}

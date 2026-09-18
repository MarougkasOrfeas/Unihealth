package gr.uniwa.unihealth.backend.dto;

import gr.uniwa.unihealth.backend.model.enums.Gender;
import gr.uniwa.unihealth.backend.model.enums.PrimaryGoal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The main health form.
 *
 * <p>Every field stays optional — a user may complete the profile without giving a height — but
 * anything they do send has to be physically plausible. These bounds are not cosmetic: height and
 * weight feed the BMI used for labelling, and an unchecked zero height previously produced an
 * infinite BMI that satisfied "BMI >= 35" and awarded the highest-priority label in the system.
 * Both endpoints that accept this DTO are already annotated {@code @Valid}.
 */
@Getter
@Setter
public class HealthProfileDTO extends BaseUpdatableDTO {

  @Past
  private LocalDate dateOfBirth;

  private Gender gender;

  /**
   * Bounds match the health form's own validators, so the backend never rejects a value the UI
   * presented as valid. The lower bound is the one that matters most here: it is what keeps the
   * derived BMI finite.
   */
  @Min(50)
  @Max(300)
  private Integer heightCm;

  @DecimalMin("20.0")
  @DecimalMax("500.0")
  private BigDecimal weightKg;

  private boolean hasFoodAllergies;

  @Size(max = 1000)
  private String foodAllergiesDetails;

  private boolean hasChronicConditions;

  @Size(max = 1000)
  private String chronicConditionsDetails;

  private PrimaryGoal primaryGoal;
}

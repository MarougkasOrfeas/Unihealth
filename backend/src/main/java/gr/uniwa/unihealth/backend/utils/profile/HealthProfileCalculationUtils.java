package gr.uniwa.unihealth.backend.utils.profile;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@UtilityClass
public class HealthProfileCalculationUtils {

  public static Integer calculateAge(LocalDate dateOfBirth) {
    if (dateOfBirth == null) {
      return null;
    }
    return Period.between(dateOfBirth, LocalDate.now()).getYears();
  }

  public static BigDecimal calculateBmi(Integer heightCm, BigDecimal weightKg) {
    if (heightCm == null || weightKg == null || heightCm <= 0) {
      return null;
    }

    BigDecimal heightM =
        BigDecimal.valueOf(heightCm).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

    BigDecimal bmi = weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    return bmi;
  }
}

package gr.uniwa.unihealth.backend.service.personalization.util;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.model.LabelFieldMapping;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class LabelEvaluationUtils {

  public static Map<String, String> buildValueMap(HealthProfileDTO dto) {
    Map<String, String> values = new HashMap<>();

    values.put("gender", dto.getGender() != null ? dto.getGender().name() : null);
    values.put("primaryGoal", dto.getPrimaryGoal() != null ? dto.getPrimaryGoal().name() : null);

    values.put("heightCm", dto.getHeightCm() != null ? String.valueOf(dto.getHeightCm()) : null);
    values.put("weightKg", dto.getWeightKg() != null ? dto.getWeightKg().toPlainString() : null);

    values.put("hasFoodAllergies", String.valueOf(dto.isHasFoodAllergies()));
    values.put("hasChronicConditions", String.valueOf(dto.isHasChronicConditions()));

    if (dto.getDateOfBirth() != null) {
      int age = Period.between(dto.getDateOfBirth(), LocalDate.now()).getYears();
      values.put("age", String.valueOf(age));
    }

    if (dto.getHeightCm() != null && dto.getWeightKg() != null) {
      double heightM = dto.getHeightCm() / 100.0;
      double bmi = dto.getWeightKg().doubleValue() / (heightM * heightM);
      values.put("bmi", String.format("%.2f", bmi));
    }

    return values;
  }

  public static boolean matches(LabelFieldMapping rule, Map<String, String> values) {
    String raw = values.get(rule.getFieldName());

    if (raw == null)
      return false;

    return switch (rule.getOperator()) {
      case "EQ" -> raw.equalsIgnoreCase(rule.getMatchValue());
      case "LT" -> toDouble(raw) < toDouble(rule.getMatchValue());
      case "LTE" -> toDouble(raw) <= toDouble(rule.getMatchValue());
      case "GT" -> toDouble(raw) > toDouble(rule.getMatchValue());
      case "GTE" -> toDouble(raw) >= toDouble(rule.getMatchValue());
      case "BETWEEN" ->
          toDouble(raw) >= toDouble(rule.getMatchValue()) && toDouble(raw) <= toDouble(
              rule.getMatchValue2());
      default -> false;
    };
  }

  private double toDouble(String value) {
    return Double.parseDouble(value);
  }

  public static void applyRules(List<String> labels, String goal, boolean hasChronic,
      boolean hasAllergy, String gender) {
    if (labels.contains("AGE_YOUNG_ADULT") && labels.contains("BMI_OVERWEIGHT"))
      labels.add("YOUNG_OVERWEIGHT");
    if (labels.contains("AGE_SENIOR") && labels.contains("BMI_UNDERWEIGHT"))
      labels.add("SENIOR_UNDERWEIGHT");
    if (labels.contains("AGE_SENIOR") && labels.contains("BMI_OBESE"))
      labels.add("SENIOR_OBESE");
    if (labels.contains("AGE_TEEN") && labels.contains("BMI_UNDERWEIGHT"))
      labels.add("TEEN_UNDERWEIGHT");
    if (labels.contains("AGE_MIDDLE_AGED") && labels.contains("BMI_OVERWEIGHT"))
      labels.add("MIDDLE_AGED_OVERWEIGHT");

    // BMI + Goal alignment
    if (labels.contains("BMI_OVERWEIGHT") && "MANAGE_WEIGHT".equals(goal))
      labels.add("GOAL_ALIGNED_MANAGE_WEIGHT");
    if (labels.contains("BMI_UNDERWEIGHT") && "MANAGE_WEIGHT".equals(goal))
      labels.add("GOAL_MISALIGNED_MANAGE_WEIGHT");
    if (labels.contains("BMI_NORMAL") && "IMPROVE_FITNESS".equals(goal))
      labels.add("GOAL_ALIGNED_IMPROVE_FITNESS");
    if (labels.contains("BMI_NORMAL") && "EAT_HEALTHIER".equals(goal))
      labels.add("GOAL_ALIGNED_EAT_HEALTHIER");
    if (labels.contains("BMI_OBESE") && "IMPROVE_FITNESS".equals(goal))
      labels.add("GOAL_MISALIGNED_IMPROVE_FITNESS");

    // BMI + Chronic
    if (labels.contains("BMI_OVERWEIGHT") && hasChronic)
      labels.add("ELEVATED_HEALTH_RISK");
    if (labels.contains("BMI_OBESE") && hasChronic)
      labels.add("METABOLIC_RISK");
    if (labels.contains("BMI_OBESE") && hasChronic)
      labels.add("HIGH_RISK_PROFILE");
    if (labels.contains("BMI_NORMAL") && !hasChronic && !hasAllergy)
      labels.add("LOW_RISK_PROFILE");

    // Age + Chronic
    if (labels.contains("AGE_YOUNG_ADULT") && hasChronic)
      labels.add("YOUNG_CHRONIC_PATIENT");
    if (labels.contains("AGE_SENIOR") && hasChronic)
      labels.add("SENIOR_CHRONIC_PATIENT");
    if (labels.contains("AGE_SENIOR") && hasChronic && hasAllergy)
      labels.add("SENIOR_COMPLEX_PROFILE");
    if (labels.contains("AGE_MIDDLE_AGED") && hasChronic)
      labels.add("MIDDLE_AGED_CHRONIC");

    // Goal + Allergy / Chronic
    if (hasAllergy && "EAT_HEALTHIER".equals(goal))
      labels.add("NUTRITION_SENSITIVE");
    if (hasAllergy && "MANAGE_WEIGHT".equals(goal))
      labels.add("WEIGHT_MGMT_WITH_ALLERGY");
    if (hasAllergy && "IMPROVE_FITNESS".equals(goal))
      labels.add("FITNESS_WITH_ALLERGY");
    if (hasChronic && "REDUCE_STRESS".equals(goal))
      labels.add("STRESS_WITH_CHRONIC");
    if (hasChronic && "IMPROVE_FITNESS".equals(goal))
      labels.add("FITNESS_WITH_CHRONIC");
    if (hasChronic && "SLEEP_BETTER".equals(goal))
      labels.add("SLEEP_WITH_CHRONIC");

    // Gender + Goal
    if ("FEMALE".equals(gender) && "MANAGE_WEIGHT".equals(goal))
      labels.add("FEMALE_MANAGE_WEIGHT");
    if ("FEMALE".equals(gender) && "IMPROVE_FITNESS".equals(goal))
      labels.add("FEMALE_IMPROVE_FITNESS");
    if ("FEMALE".equals(gender) && "REDUCE_STRESS".equals(goal))
      labels.add("FEMALE_REDUCE_STRESS");
    if ("FEMALE".equals(gender) && "EAT_HEALTHIER".equals(goal))
      labels.add("FEMALE_EAT_HEALTHIER");
    if ("FEMALE".equals(gender) && "SLEEP_BETTER".equals(goal))
      labels.add("FEMALE_SLEEP_BETTER");
    if ("MALE".equals(gender) && "MANAGE_WEIGHT".equals(goal))
      labels.add("MALE_MANAGE_WEIGHT");
    if ("MALE".equals(gender) && "IMPROVE_FITNESS".equals(goal))
      labels.add("MALE_IMPROVE_FITNESS");
    if ("MALE".equals(gender) && "REDUCE_STRESS".equals(goal))
      labels.add("MALE_REDUCE_STRESS");
    if ("MALE".equals(gender) && "GENERAL_WELLBEING".equals(goal))
      labels.add("MALE_GENERAL_WELLBEING");
    if ("MALE".equals(gender) && "SLEEP_BETTER".equals(goal))
      labels.add("MALE_SLEEP_BETTER");
  }
}

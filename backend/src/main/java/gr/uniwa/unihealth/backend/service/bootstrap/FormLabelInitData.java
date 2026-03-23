package gr.uniwa.unihealth.backend.service.bootstrap;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.LabelFieldMapping;
import gr.uniwa.unihealth.backend.repository.LabelFieldMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FormLabelInitData implements ApplicationRunner {

  private final TenantContext tenantContext;
  private final LabelFieldMappingRepository repository;

  @Override
  public void run(ApplicationArguments args) {
    tenantContext.runForEachTenant(this::initialize, "FormLabelInitData.run");
  }

  public void initialize(String tenantId) {
    if (repository.count() > 0) {
      log.info("Form Label Init Data already exists for tenant [{}]. Skipping initialization.",
          tenantId);
      return;
    }
    log.info("Initializing Form Label Init Data content for tenant [{}].", tenantId);
    addInitData();
  }

  private void addInitData() {
    List<LabelFieldMapping> mappings = new ArrayList<>();

    // ================================================================
    // TYPE 1 — Pure field rules
    // ================================================================
    // gender
    mappings.add(build("GENDER_MALE", "gender", "EQ", "Male", null, false, false, "gender",
        "User identified as Male"));
    mappings.add(build("GENDER_FEMALE", "gender", "EQ", "Female", null, false, false, "gender",
        "User identified as Female"));
    mappings.add(build("GENDER_OTHER", "gender", "EQ", "Other", null, false, false, "gender",
        "User identified as Other"));

    // height
    mappings.add(build("HEIGHT_VERY_SHORT", "heightCm", "LTE", "155", null, false, false, "height",
        "Height <= 155cm"));
    mappings.add(build("HEIGHT_SHORT", "heightCm", "BETWEEN", "156", "165", false, false, "height",
        "Height 156-165cm"));
    mappings.add(
        build("HEIGHT_AVERAGE", "heightCm", "BETWEEN", "166", "175", false, false, "height",
            "Height 166-175cm"));
    mappings.add(build("HEIGHT_TALL", "heightCm", "BETWEEN", "176", "189", false, false, "height",
        "Height 176-189cm"));
    mappings.add(build("HEIGHT_VERY_TALL", "heightCm", "GTE", "190", null, false, false, "height",
        "Height >= 190cm"));

    // weight
    mappings.add(build("WEIGHT_VERY_LOW", "weightKg", "LTE", "45", null, false, false, "weight",
        "Weight <= 45kg"));
    mappings.add(build("WEIGHT_LOW", "weightKg", "BETWEEN", "46", "60", false, false, "weight",
        "Weight 46-60kg"));
    mappings.add(build("WEIGHT_AVERAGE", "weightKg", "BETWEEN", "61", "80", false, false, "weight",
        "Weight 61-80kg"));
    mappings.add(build("WEIGHT_HIGH", "weightKg", "BETWEEN", "81", "99", false, false, "weight",
        "Weight 81-99kg"));
    mappings.add(build("WEIGHT_VERY_HIGH", "weightKg", "GTE", "100", null, false, false, "weight",
        "Weight >= 100kg"));

    // allergy
    mappings.add(
        build("HAS_FOOD_ALLERGY", "hasFoodAllergies", "EQ", "true", null, false, false, "allergy",
            "User has food allergies"));
    mappings.add(
        build("NO_FOOD_ALLERGY", "hasFoodAllergies", "EQ", "false", null, false, false, "allergy",
            "User has no food allergies"));

    // chronic
    mappings.add(
        build("HAS_CHRONIC_CONDITION", "hasChronicConditions", "EQ", "true", null, false, false,
            "chronic", "User has chronic conditions"));
    mappings.add(
        build("NO_CHRONIC_CONDITION", "hasChronicConditions", "EQ", "false", null, false, false,
            "chronic", "User has no chronic conditions"));

    // goal
    mappings.add(
        build("GOAL_EAT_HEALTHIER", "primaryGoal", "EQ", "EAT_HEALTHIER", null, false, false,
            "goal", "Primary goal: Eat Healthier"));
    mappings.add(
        build("GOAL_IMPROVE_FITNESS", "primaryGoal", "EQ", "IMPROVE_FITNESS", null, false, false,
            "goal", "Primary goal: Improve Fitness"));
    mappings.add(
        build("GOAL_MANAGE_WEIGHT", "primaryGoal", "EQ", "MANAGE_WEIGHT", null, false, false,
            "goal", "Primary goal: Manage Weight"));
    mappings.add(
        build("GOAL_SLEEP_BETTER", "primaryGoal", "EQ", "SLEEP_BETTER", null, false, false, "goal",
            "Primary goal: Sleep Better"));
    mappings.add(
        build("GOAL_REDUCE_STRESS", "primaryGoal", "EQ", "REDUCE_STRESS", null, false, false,
            "goal", "Primary goal: Reduce Stress"));
    mappings.add(
        build("GOAL_GENERAL_WELLBEING", "primaryGoal", "EQ", "GENERAL_WELLBEING", null, false,
            false, "goal", "Primary goal: General Wellbeing"));


    // ================================================================
    // TYPE 2 — Calculated rules
    // ================================================================
    // age
    mappings.add(build("AGE_TEEN", "age", "BETWEEN", "13", "17", true, false, "age",
        "User is a teenager (13-17)"));
    mappings.add(build("AGE_YOUNG_ADULT", "age", "BETWEEN", "18", "35", true, false, "age",
        "User is a young adult (18-35)"));
    mappings.add(build("AGE_MIDDLE_AGED", "age", "BETWEEN", "36", "55", true, false, "age",
        "User is middle-aged (36-55)"));
    mappings.add(build("AGE_SENIOR", "age", "GTE", "56", null, true, false, "age",
        "User is a senior (56+)"));

    // bmi
    mappings.add(build("BMI_UNDERWEIGHT", "bmi", "LT", "18.5", null, true, false, "bmi",
        "BMI below 18.5 - Underweight"));
    mappings.add(build("BMI_NORMAL", "bmi", "BETWEEN", "18.5", "24.9", true, false, "bmi",
        "BMI 18.5-24.9 - Normal weight"));
    mappings.add(build("BMI_OVERWEIGHT", "bmi", "BETWEEN", "25.0", "29.9", true, false, "bmi",
        "BMI 25.0-29.9 - Overweight"));
    mappings.add(build("BMI_OBESE", "bmi", "BETWEEN", "30.0", "34.9", true, false, "bmi",
        "BMI 30.0-34.9 - Obese"));
    mappings.add(build("BMI_SEVERELY_OBESE", "bmi", "GTE", "35.0", null, true, false, "bmi",
        "BMI 35.0+ - Severely obese"));

    // ================================================================
    // TYPE 3 — Mixed rules
    // ================================================================

    // BMI + Age
    mappings.add(build("YOUNG_OVERWEIGHT", "bmi+age", null, null, null, false, true, "mixed",
        "Young adult (18-35) with BMI >= 25"));
    mappings.add(build("SENIOR_UNDERWEIGHT", "bmi+age", null, null, null, false, true, "mixed",
        "Senior (56+) with BMI below 18.5"));
    mappings.add(build("SENIOR_OBESE", "bmi+age", null, null, null, false, true, "mixed",
        "Senior (56+) with BMI >= 30"));
    mappings.add(build("TEEN_UNDERWEIGHT", "bmi+age", null, null, null, false, true, "mixed",
        "Teenager (13-17) with BMI below 18.5"));
    mappings.add(build("MIDDLE_AGED_OVERWEIGHT", "bmi+age", null, null, null, false, true, "mixed",
        "Middle-aged (36-55) with BMI >= 25"));

    // BMI + Goal
    mappings.add(
        build("GOAL_ALIGNED_MANAGE_WEIGHT", "bmi+goal", null, null, null, false, true, "mixed",
            "Overweight user targeting weight management - aligned"));
    mappings.add(
        build("GOAL_MISALIGNED_MANAGE_WEIGHT", "bmi+goal", null, null, null, false, true, "mixed",
            "Underweight user targeting weight management - misaligned"));
    mappings.add(
        build("GOAL_ALIGNED_IMPROVE_FITNESS", "bmi+goal", null, null, null, false, true, "mixed",
            "Normal BMI user targeting fitness improvement - aligned"));
    mappings.add(
        build("GOAL_ALIGNED_EAT_HEALTHIER", "bmi+goal", null, null, null, false, true, "mixed",
            "Normal BMI user targeting healthier eating - aligned"));
    mappings.add(
        build("GOAL_MISALIGNED_IMPROVE_FITNESS", "bmi+goal", null, null, null, false, true, "mixed",
            "Obese user targeting fitness - consider weight first"));

    // BMI + Chronic
    mappings.add(
        build("ELEVATED_HEALTH_RISK", "bmi+chronic", null, null, null, false, true, "mixed",
            "Overweight user with chronic condition"));
    mappings.add(build("METABOLIC_RISK", "bmi+chronic", null, null, null, false, true, "mixed",
        "Obese user with chronic condition"));
    mappings.add(build("HIGH_RISK_PROFILE", "bmi+chronic", null, null, null, false, true, "mixed",
        "Severely obese + chronic condition - high risk"));
    mappings.add(build("LOW_RISK_PROFILE", "bmi+chronic", null, null, null, false, true, "mixed",
        "Normal BMI, no chronic, no allergy - low risk"));

    // Age + Chronic
    mappings.add(
        build("YOUNG_CHRONIC_PATIENT", "age+chronic", null, null, null, false, true, "mixed",
            "Young adult with chronic condition"));
    mappings.add(
        build("SENIOR_CHRONIC_PATIENT", "age+chronic", null, null, null, false, true, "mixed",
            "Senior with chronic condition"));
    mappings.add(
        build("SENIOR_COMPLEX_PROFILE", "age+chronic", null, null, null, false, true, "mixed",
            "Senior with chronic condition and food allergy"));
    mappings.add(build("MIDDLE_AGED_CHRONIC", "age+chronic", null, null, null, false, true, "mixed",
        "Middle-aged user with chronic condition"));

    // Goal + Allergy/Chronic
    mappings.add(
        build("NUTRITION_SENSITIVE", "goal+allergy", null, null, null, false, true, "mixed",
            "User with allergy targeting healthier eating"));
    mappings.add(
        build("WEIGHT_MGMT_WITH_ALLERGY", "goal+allergy", null, null, null, false, true, "mixed",
            "User with allergy targeting weight management"));
    mappings.add(
        build("FITNESS_WITH_ALLERGY", "goal+allergy", null, null, null, false, true, "mixed",
            "User with allergy targeting fitness improvement"));
    mappings.add(
        build("STRESS_WITH_CHRONIC", "goal+chronic", null, null, null, false, true, "mixed",
            "User with chronic condition targeting stress reduction"));
    mappings.add(
        build("FITNESS_WITH_CHRONIC", "goal+chronic", null, null, null, false, true, "mixed",
            "User with chronic condition targeting fitness"));
    mappings.add(build("SLEEP_WITH_CHRONIC", "goal+chronic", null, null, null, false, true, "mixed",
        "User with chronic condition targeting better sleep"));

    // Gender + Goal
    mappings.add(
        build("FEMALE_MANAGE_WEIGHT", "gender+goal", null, null, null, false, true, "mixed",
            "Female user targeting weight management"));
    mappings.add(
        build("FEMALE_IMPROVE_FITNESS", "gender+goal", null, null, null, false, true, "mixed",
            "Female user targeting fitness improvement"));
    mappings.add(
        build("FEMALE_REDUCE_STRESS", "gender+goal", null, null, null, false, true, "mixed",
            "Female user targeting stress reduction"));
    mappings.add(
        build("FEMALE_EAT_HEALTHIER", "gender+goal", null, null, null, false, true, "mixed",
            "Female user targeting healthier eating"));
    mappings.add(build("FEMALE_SLEEP_BETTER", "gender+goal", null, null, null, false, true, "mixed",
        "Female user targeting better sleep"));
    mappings.add(build("MALE_MANAGE_WEIGHT", "gender+goal", null, null, null, false, true, "mixed",
        "Male user targeting weight management"));
    mappings.add(
        build("MALE_IMPROVE_FITNESS", "gender+goal", null, null, null, false, true, "mixed",
            "Male user targeting fitness improvement"));
    mappings.add(build("MALE_REDUCE_STRESS", "gender+goal", null, null, null, false, true, "mixed",
        "Male user targeting stress reduction"));
    mappings.add(
        build("MALE_GENERAL_WELLBEING", "gender+goal", null, null, null, false, true, "mixed",
            "Male user targeting general wellbeing"));
    mappings.add(build("MALE_SLEEP_BETTER", "gender+goal", null, null, null, false, true, "mixed",
        "Male user targeting better sleep"));

    repository.saveAll(mappings);
  }

  private LabelFieldMapping build(String labelCode, String fieldName, String operator,
      String matchValue, String matchValue2, boolean calculatedField, boolean mixedRule,
      String labelGroup, String description) {
    LabelFieldMapping m = new LabelFieldMapping();
    m.setLabelCode(labelCode);
    m.setFieldName(fieldName);
    m.setOperator(operator);
    m.setMatchValue(matchValue);
    m.setMatchValue2(matchValue2);
    m.setCalculatedField(calculatedField);
    m.setMixedRule(mixedRule);
    m.setLabelGroup(labelGroup);
    m.setDescription(description);
    m.setPriority(
        calculateSignificancePriority(labelCode, labelGroup, operator, matchValue, mixedRule));
    m.setActive(true);
    return m;
  }

  /**
   * Significance Score Calculator
   * <p>
   * Based on a simplified Analytic Hierarchy Process (AHP) Thomas L. Saaty in 1977  — a
   * multi-criteria decision method that assigns relative importance weights across independent
   * dimensions, then combines them multiplicatively into a single significance score. </p>
   * <p>
   * The three dimensions are: 1. Domain Tier     — how clinically/behaviourally actionable the
   * label group is 2. Specificity     — how precise/extreme the condition is within its group 3.
   * Urgency Factor  — whether the label implies immediate attention needed </p>
   */
  private int calculateSignificancePriority(String labelCode, String labelGroup, String operator,
      String matchValue, boolean mixedRule) {

    // ── DIMENSION 1: Domain Tier Weight (AHP — pairwise importance) ───────────
    //
    // Groups ranked by how actionable they are for content recommendation.
    // A health risk drives content far more than a demographic descriptor.
    //
    // Tier 5 (critical)   — immediate health concern, drives all content
    // Tier 4 (high)       — strong health signal, major content influence
    // Tier 3 (moderate)   — contextual health info, secondary content shaping
    // Tier 2 (low)        — personalisation signal, minor content shaping
    // Tier 1 (minimal)    — demographic label, used only for tone/framing
    //
    int tierWeight = switch (labelGroup) {
      case "bmi" -> 500; // tier 5 — most clinically actionable
      case "mixed" -> 450; // tier 5 — multi-signal, highest specificity
      case "chronic" -> 400; // tier 4 — direct health condition
      case "allergy" -> 350; // tier 4 — direct dietary constraint
      case "weight" -> 300; // tier 3 — physical metric, actionable
      case "age" -> 200; // tier 2 — contextual, shapes tone
      case "height" -> 150; // tier 2 — low actionability
      case "goal" -> 100; // tier 2 — stated intent, content shaping
      case "gender" -> 50; // tier 1 — demographic, framing only
      default -> 10;
    };

    // ── DIMENSION 2: Specificity Multiplier ───────────────────────────────────
    //
    // Within the same group, a more extreme or specific value
    // is more significant and should drive stronger content response.
    //
    // Example: BMI_SEVERELY_OBESE is more urgent than BMI_OVERWEIGHT
    //          WEIGHT_VERY_LOW is more urgent than WEIGHT_LOW
    //
    // This is derived from the label_code suffix and operator combination.
    // Multiplier is a float applied to the tier weight.
    //
    double specificityMultiplier = resolveSpecificityMultiplier(labelCode);

    // ── DIMENSION 3: Urgency Factor ───────────────────────────────────────────
    //
    // Some labels imply an immediate actionable concern (positive flag),
    // others are neutral descriptors. A user WITH a condition needs
    // stronger content signal than a user WITHOUT one.
    //
    // HAS_CHRONIC_CONDITION  → urgency 1.5 (needs content now)
    // NO_CHRONIC_CONDITION   → urgency 0.5 (neutral, reduces weight)
    // GOAL_WEIGHT_LOSS       → urgency 1.2 (stated active intent)
    // GENDER_MALE            → urgency 1.0 (neutral descriptor)
    //
    double urgencyFactor = resolveUrgencyFactor(labelCode);

    return (int) (tierWeight * specificityMultiplier * urgencyFactor);
  }

  /**
   * Resolves how extreme/specific this label is within its group. "VERY" extreme conditions get a
   * higher multiplier than moderate ones.
   *
   * Scale: 0.5 (very mild) → 1.0 (moderate) → 2.0 (extreme)
   */
  private double resolveSpecificityMultiplier(String labelCode) {
    // Extreme conditions — strongest content signal
    if (labelCode.contains("SEVERELY") || labelCode.contains("VERY_LOW") || labelCode.contains(
        "VERY_HIGH") || labelCode.contains("VERY_SHORT") || labelCode.contains(
        "VERY_TALL") || labelCode.contains("HIGH_RISK") || labelCode.contains("MISALIGNED"))
      return 2.0;

    // Strong conditions
    if (labelCode.contains("OBESE") || labelCode.contains("UNDERWEIGHT") || labelCode.contains(
        "METABOLIC") || labelCode.contains("COMPLEX"))
      return 1.8;

    // Moderate conditions
    if (labelCode.contains("OVERWEIGHT") || labelCode.contains("ELEVATED") || labelCode.contains(
        "CHRONIC") || labelCode.contains("ALLERGY") || labelCode.contains(
        "RESTRICTIONS") || labelCode.contains("ALIGNED"))
      return 1.3;

    // Mild / neutral conditions
    if (labelCode.contains("NORMAL") || labelCode.contains("AVERAGE") || labelCode.contains(
        "LOW_RISK"))
      return 0.8;

    // Pure demographic / descriptive labels
    if (labelCode.startsWith("GENDER_") || labelCode.startsWith("AGE_") || labelCode.startsWith(
        "HEIGHT_"))
      return 0.6;

    return 1.0; // default — no modifier
  }

  /**
   * Resolves the urgency of acting on this label. Positive health flags (HAS_, risk profiles) carry
   * higher urgency than neutral descriptors or negative flags (NO_).
   *
   * Scale: 0.5 (irrelevant/negative) → 1.0 (neutral) → 1.5 (urgent)
   */
  private double resolveUrgencyFactor(String labelCode) {

    // Immediate concern — must drive content strongly
    if (labelCode.startsWith("HAS_") || labelCode.contains("RISK") || labelCode.contains(
        "MISALIGNED") || labelCode.contains("SEVERELY") || labelCode.contains("COMPLEX_PROFILE"))
      return 1.5;

    // Active intent — user stated they want to act
    if (labelCode.startsWith("GOAL_") || labelCode.contains("ALIGNED") || labelCode.contains(
        "SENSITIVE") || labelCode.contains("WITH_CHRONIC") || labelCode.contains(
        "WITH_RESTRICTIONS"))
      return 1.2;

    // Neutral descriptor — useful for personalisation, not urgency
    if (labelCode.startsWith("GENDER_") || labelCode.startsWith("HEIGHT_") || labelCode.startsWith(
        "AGE_"))
      return 1.0;

    // Negative flags — user does NOT have the condition
    // Still useful but should not drive strong content signals
    if (labelCode.startsWith("NO_"))
      return 0.5;

    return 1.0;
  }
}

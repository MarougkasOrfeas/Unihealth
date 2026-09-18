package gr.uniwa.unihealth.backend.service.personalization.impl;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;
import gr.uniwa.unihealth.backend.model.LabelFieldMapping;
import gr.uniwa.unihealth.backend.repository.LabelFieldMappingRepository;
import gr.uniwa.unihealth.backend.service.personalization.LabelEvaluatorService;
import gr.uniwa.unihealth.backend.service.personalization.LabelPriorityResolver;
import gr.uniwa.unihealth.backend.service.personalization.resolver.HealthDetailLabelExtractor;
import gr.uniwa.unihealth.backend.service.personalization.util.LabelEvaluationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelEvaluatorServiceImpl implements LabelEvaluatorService {

  private final LabelFieldMappingRepository repository;
  private final HealthDetailLabelExtractor detailExtractor;
  private final LabelPriorityResolver priorityResolver;

  @Override
  public List<String> evaluateAndSort(HealthProfileDTO dto) {
    List<String> labels = evaluate(dto);
    return sortBySignificance(labels);
  }

  @Override
  public List<String> evaluateAndSortOptional(OptionalHealthProfileDTO dto) {
    List<String> labels = evaluateOptional(dto);
    return sortBySignificance(labels);
  }

  private List<String> evaluate(HealthProfileDTO dto) {
    Map<String, String> values = LabelEvaluationUtils.buildValueMap(dto);
    List<String> labels = new ArrayList<>();

    // field rules
    repository.findByActiveTrueAndCalculatedFieldFalseAndMixedRuleFalse().stream()
        .filter(rule -> LabelEvaluationUtils.matches(rule, values))
        .map(LabelFieldMapping::getLabelCode).forEach(labels::add);

    // calculated rules
    repository.findByActiveTrueAndCalculatedFieldTrueAndMixedRuleFalse().stream()
        .filter(rule -> LabelEvaluationUtils.matches(rule, values))
        .map(LabelFieldMapping::getLabelCode).forEach(labels::add);

    // composite rules in code
    evaluateCompositeRules(labels, dto);

    // free text extraction from allergy/chronic detail fields
    List<String> detailLabels = detailExtractor.extract(dto);

    labels.addAll(detailLabels);
    return labels;
  }

  private void evaluateCompositeRules(List<String> labels, HealthProfileDTO dto) {
    boolean hasChronic = dto.isHasChronicConditions();
    boolean hasAllergy = dto.isHasFoodAllergies();

    String goal = dto.getPrimaryGoal() != null ? dto.getPrimaryGoal().name() : null;
    String gender = dto.getGender() != null ? dto.getGender().name() : null;

    LabelEvaluationUtils.applyRules(labels, goal, hasChronic, hasAllergy, gender);
  }

  /**
   * Orders labels by significance, most significant first.
   *
   * <p>Form-rule labels carry their priority on {@link LabelFieldMapping}. The specific medical
   * codes do not — they come from the free-text dictionary, which has no mapping row — so they used
   * to score zero here and sort last as an undifferentiated block. That made the per-concept
   * priorities in {@code medical-terms.json} half-wired: reported correctly when the profile was
   * read back, but ignored when deciding the stored order, so a migraine could sit ahead of
   * diabetes.
   */
  private List<String> sortBySignificance(List<String> labelCodes) {
    Map<String, Integer> scoreMap = repository.findByLabelCodeInAndActiveTrue(labelCodes).stream()
        .collect(Collectors.toMap(LabelFieldMapping::getLabelCode, LabelFieldMapping::getPriority,
            (first, duplicate) -> first));

    return labelCodes.stream()
        .sorted(Comparator.comparingInt(code -> -significanceOf(code, scoreMap)))
        .collect(Collectors.toList());
  }

  private int significanceOf(String labelCode, Map<String, Integer> scoreMap) {
    Integer fromMapping = scoreMap.get(labelCode);
    return fromMapping != null ? fromMapping : priorityResolver.priorityOf(labelCode);
  }

  private List<String> evaluateOptional(OptionalHealthProfileDTO dto) {
    List<String> labels = new ArrayList<>();

    if (dto.getSleepQuality() != null) {
      labels.add("OPTIONAL_SLEEP_" + dto.getSleepQuality());
    }

    if (dto.getStudyLoad() != null) {
      labels.add("OPTIONAL_STUDY_LOAD_" + dto.getStudyLoad());
    }

    if (dto.getSmoking() != null) {
      labels.add("OPTIONAL_SMOKING_" + dto.getSmoking());
    }

    if (dto.getCoffee() != null) {
      labels.add("OPTIONAL_COFFEE_" + dto.getCoffee());
    }

    if (dto.getScreenTime() != null) {
      labels.add("OPTIONAL_SCREEN_TIME_" + dto.getScreenTime());
    }

    if (dto.getExercise() != null) {
      labels.add("OPTIONAL_EXERCISE_" + dto.getExercise());
    }

    if (dto.getMealsPerDay() != null) {
      labels.add("OPTIONAL_MEALS_" + dto.getMealsPerDay());
    }

    if (dto.getEatSnack() != null) {
      labels.add("OPTIONAL_SNACK_" + dto.getEatSnack());
    }

    if (dto.getWater() != null) {
      labels.add("OPTIONAL_WATER_" + dto.getWater());
    }

    if (dto.getDietType() != null) {
      labels.add("OPTIONAL_DIET_" + dto.getDietType());
    }

    if (Boolean.TRUE.equals(dto.getMedication())) {
      labels.add("OPTIONAL_HIGH_MEDICATION");
    }

    if (Boolean.TRUE.equals(dto.getSurgeryHistory())) {
      labels.add("OPTIONAL_HIGH_SURGERY_HISTORY");
    }

    if (dto.getPreferredContent() != null) {
      labels.add("OPTIONAL_CONTENT_" + dto.getPreferredContent());
    }

    if (dto.getFrequency() != null) {
      labels.add("OPTIONAL_FREQUENCY_" + dto.getFrequency());
    }

    return labels;
  }
}

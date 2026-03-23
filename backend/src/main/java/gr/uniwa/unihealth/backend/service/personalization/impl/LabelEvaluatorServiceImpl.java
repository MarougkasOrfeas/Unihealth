package gr.uniwa.unihealth.backend.service.personalization.impl;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.model.LabelFieldMapping;
import gr.uniwa.unihealth.backend.repository.LabelFieldMappingRepository;
import gr.uniwa.unihealth.backend.service.personalization.LabelEvaluatorService;
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

  @Override
  public List<String> evaluateAndSort(HealthProfileDTO dto) {
    List<String> labels = evaluate(dto);
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

  private List<String> sortBySignificance(List<String> labelCodes) {
    Map<String, Integer> scoreMap = repository.findByLabelCodeInAndActiveTrue(labelCodes).stream()
        .collect(Collectors.toMap(LabelFieldMapping::getLabelCode, LabelFieldMapping::getPriority));

    return labelCodes.stream()
        .sorted(Comparator.comparingInt(code -> -scoreMap.getOrDefault(code, 0)))
        .collect(Collectors.toList());
  }
}

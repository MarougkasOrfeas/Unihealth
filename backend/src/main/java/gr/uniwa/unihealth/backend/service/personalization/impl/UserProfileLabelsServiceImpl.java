package gr.uniwa.unihealth.backend.service.personalization.impl;

import gr.uniwa.unihealth.backend.dto.UserProfileLabelDTO;
import gr.uniwa.unihealth.backend.model.LabelFieldMapping;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.UserProfileLabels;
import gr.uniwa.unihealth.backend.repository.LabelFieldMappingRepository;
import gr.uniwa.unihealth.backend.repository.UserProfileLabelsRepository;
import gr.uniwa.unihealth.backend.service.personalization.UserProfileLabelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileLabelsServiceImpl implements UserProfileLabelsService {

  private final UserProfileLabelsRepository repository;
  private final LabelFieldMappingRepository labelFieldMappingRepository;

  @Override
  public void saveForUser(User user, List<String> sortedLabels) {
    UserProfileLabels entity =
        repository.findByUserId(user.getId()).orElse(new UserProfileLabels());

    entity.setUser(user);
    entity.setLabels(finalPrioritize(sortedLabels));

    repository.save(entity);
  }

  @Override
  public List<UserProfileLabelDTO> findForUser(String userId) {
    List<String> labels = repository.findByUserId(userId)
        .map(UserProfileLabels::getLabels)
        .orElse(List.of());

    Map<String, Integer> priorities =
        labelFieldMappingRepository.findByLabelCodeInAndActiveTrue(labels).stream()
            .collect(Collectors.toMap(LabelFieldMapping::getLabelCode, LabelFieldMapping::getPriority));

    return labels.stream()
        .map(label -> new UserProfileLabelDTO(label, priorities.getOrDefault(label,
            resolveFallbackPriority(label))))
        .toList();
  }

  private int resolveFallbackPriority(String label) {
    if (label.startsWith("CHRONIC_")) {
      return 1200;
    }
    if (label.startsWith("ALLERGY_")) {
      return 1000;
    }
    if (label.startsWith("OPTIONAL_HIGH_")) {
      return 900;
    }
    if (label.startsWith("OPTIONAL_")) {
      return 200;
    }
    return 50;
  }

  @Override
  public void saveOptionalForUser(User user, List<String> sortedOptionalLabels) {
    UserProfileLabels entity =
        repository.findByUserId(user.getId()).orElse(new UserProfileLabels());

    entity.setUser(user);

    List<String> existingLabels =
        entity.getLabels() != null ? entity.getLabels() : new ArrayList<>();

    List<String> mergedLabels = new ArrayList<>();

    existingLabels.stream().filter(label -> !label.startsWith("OPTIONAL_"))
        .forEach(mergedLabels::add);

    mergedLabels.addAll(sortedOptionalLabels);

    List<String> uniqueLabels = new ArrayList<>(new LinkedHashSet<>(mergedLabels));

    entity.setLabels(finalPrioritize(uniqueLabels));

    repository.save(entity);
  }


  /**
   * Reorders the significance-sorted label list to guarantee that the most clinically critical
   * labels always appear first, regardless of their AHP score.
   * <p>
   * Final order: 1. Specific chronic disease labels  (CHRONIC_*)  — highest clinical priority 2.
   * Specific allergy labels          (ALLERGY_*)  — dietary safety critical 3. Everything else —
   * AHP score order preserved </p>
   */
  private List<String> finalPrioritize(List<String> sortedLabels) {
    List<String> chronic = new ArrayList<>();
    List<String> allergies = new ArrayList<>();
    List<String> optionalHigh = new ArrayList<>();
    List<String> rest = new ArrayList<>();
    List<String> optional = new ArrayList<>();

    for (String label : sortedLabels) {
      if (label.startsWith("CHRONIC_")) {
        chronic.add(label);
      } else if (label.startsWith("ALLERGY_")) {
        allergies.add(label);
      } else if (label.startsWith("OPTIONAL_HIGH_")) {
        optionalHigh.add(label);
      } else if (label.startsWith("OPTIONAL_")) {
        optional.add(label);
      } else {
        rest.add(label);
      }
    }

    List<String> result = new ArrayList<>();
    result.addAll(chronic);
    result.addAll(allergies);
    result.addAll(optionalHigh);
    result.addAll(rest);
    result.addAll(optional);

    return result;
  }
}

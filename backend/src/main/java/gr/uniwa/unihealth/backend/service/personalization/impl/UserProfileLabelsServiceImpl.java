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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileLabelsServiceImpl implements UserProfileLabelsService {

  private final UserProfileLabelsRepository repository;
  private final LabelFieldMappingRepository labelFieldMappingRepository;

  /**
   * The prefixes that decide both ownership and clinical ranking.
   *
   * <p>They are load-bearing in three places here — which form owns a label, how it is bucketed by
   * {@link #finalPrioritize}, and its fallback priority when no active mapping exists — so they are
   * named rather than repeated as literals.
   */
  private static final String CHRONIC_PREFIX = "CHRONIC_";
  private static final String ALLERGY_PREFIX = "ALLERGY_";
  private static final String OPTIONAL_HIGH_PREFIX = "OPTIONAL_HIGH_";

  /** Labels derived from the optional health form. The main form can never produce one. */
  private static final String OPTIONAL_PREFIX = "OPTIONAL_";

  @Override
  public void saveForUser(User user, List<String> sortedLabels) {
    // Retains the optional-form labels, mirroring saveOptionalForUser, which retains the main-form
    // ones. Without the mirror, any main-profile edit — even a save that changed nothing — erased
    // every OPTIONAL_* label, because evaluateAndSort works from the main DTO and structurally
    // cannot re-emit them. The optional answers themselves survived, so the form still looked
    // complete while the personalisation derived from it had gone.
    persist(user, sortedLabels, label -> label.startsWith(OPTIONAL_PREFIX));
  }

  @Override
  public List<UserProfileLabelDTO> findForUser(String userId) {
    List<String> labels = repository.findByUserId(userId)
        .map(UserProfileLabels::getLabels)
        .orElse(List.of());

    // One lookup for both priority and group, rather than two passes over the same rows.
    Map<String, LabelFieldMapping> mappings =
        labelFieldMappingRepository.findByLabelCodeInAndActiveTrue(labels).stream()
            .collect(Collectors.toMap(LabelFieldMapping::getLabelCode, mapping -> mapping,
                (first, duplicate) -> first));

    return labels.stream()
        .map(label -> {
          LabelFieldMapping mapping = mappings.get(label);
          return new UserProfileLabelDTO(
              label,
              mapping != null ? mapping.getPriority() : resolveFallbackPriority(label),
              mapping != null ? mapping.getLabelGroup() : null);
        })
        .toList();
  }

  private int resolveFallbackPriority(String label) {
    if (label.startsWith(CHRONIC_PREFIX)) {
      return 1200;
    }
    if (label.startsWith(ALLERGY_PREFIX)) {
      return 1000;
    }
    if (label.startsWith(OPTIONAL_HIGH_PREFIX)) {
      return 900;
    }
    if (label.startsWith(OPTIONAL_PREFIX)) {
      return 200;
    }
    return 50;
  }

  @Override
  public void saveOptionalForUser(User user, List<String> sortedOptionalLabels) {
    persist(user, sortedOptionalLabels, label -> !label.startsWith(OPTIONAL_PREFIX));
  }

  /**
   * Replaces the labels the calling form owns, retaining those owned by the other form.
   *
   * <p>Each form evaluates only its own questions, so a save must never be read as "these are now
   * all the labels this user has" — only as "these are this form's labels".
   *
   * @param retainExisting picks the already-stored labels belonging to the *other* form, which must
   *                       survive this write.
   */
  private void persist(User user, List<String> incoming, Predicate<String> retainExisting) {
    UserProfileLabels entity =
        repository.findByUserId(user.getId()).orElse(new UserProfileLabels());

    entity.setUser(user);

    List<String> existingLabels =
        entity.getLabels() != null ? entity.getLabels() : List.of();

    List<String> mergedLabels = new ArrayList<>(incoming);
    existingLabels.stream().filter(retainExisting).forEach(mergedLabels::add);

    // The two sets land in disjoint buckets in finalPrioritize, so which one leads here does not
    // affect the stored order. LinkedHashSet dedups without disturbing it.
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
      if (label.startsWith(CHRONIC_PREFIX)) {
        chronic.add(label);
      } else if (label.startsWith(ALLERGY_PREFIX)) {
        allergies.add(label);
      } else if (label.startsWith(OPTIONAL_HIGH_PREFIX)) {
        optionalHigh.add(label);
      } else if (label.startsWith(OPTIONAL_PREFIX)) {
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

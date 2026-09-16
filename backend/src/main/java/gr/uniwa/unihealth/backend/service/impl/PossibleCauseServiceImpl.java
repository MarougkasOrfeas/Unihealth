package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.dto.ConditionDTO;
import gr.uniwa.unihealth.backend.dto.PossibleCauseDTO;
import gr.uniwa.unihealth.backend.dto.SymptomFactorDTO;
import gr.uniwa.unihealth.backend.mapper.ConditionMapper;
import gr.uniwa.unihealth.backend.mapper.SymptomFactorMapper;
import gr.uniwa.unihealth.backend.model.SymptomCause;
import gr.uniwa.unihealth.backend.model.SymptomFactor;
import gr.uniwa.unihealth.backend.repository.SymptomCauseRepository;
import gr.uniwa.unihealth.backend.repository.SymptomFactorRepository;
import gr.uniwa.unihealth.backend.service.PossibleCauseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation for {@link PossibleCauseService}.
 *
 * <p><b>How the ranking works.</b> Each row of the NHS causes table is described by a set of
 * factors. A cause scores the share of its own description that the reader ticked:
 *
 * <pre>
 *   score(cause) = sum of weights of its matched factors / sum of weights of all its factors
 * </pre>
 *
 * <p>So a cause described by three details, two of which the reader ticked, scores 0.67. Ties on
 * score are broken by the absolute number of matches, which is what separates a cause matched three
 * details out of three from one matched a single detail out of one - both score 1.0, but the first
 * rests on more evidence.
 *
 * <p>Nothing is inferred beyond the source. There is no model here and no probability: the number
 * is the proportion of a published description that the reader recognised, which is why the matched
 * factors are returned alongside it for the UI to show.
 *
 * @author omaro
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PossibleCauseServiceImpl implements PossibleCauseService {

  private final SymptomCauseRepository causeRepository;
  private final SymptomFactorRepository factorRepository;
  private final SymptomFactorMapper factorMapper;
  private final ConditionMapper conditionMapper;

  @Override
  public List<SymptomFactorDTO> findFactors(String slug) {
    return factorRepository.findBySymptomItemSlugOrderByDisplayOrderAsc(slug).stream()
        .map(factorMapper::mapToDTO)
        .toList();
  }

  @Override
  public List<PossibleCauseDTO> findPossibleCauses(String slug, Collection<String> factorCodes) {
    Set<String> selected = factorCodes == null ? Set.of() : new HashSet<>(factorCodes);

    List<PossibleCauseDTO> causes = new ArrayList<>();
    for (SymptomCause cause : causeRepository.findBySymptomSlugWithDetails(slug)) {
      causes.add(score(cause, selected));
    }

    causes.sort(Comparator.comparingDouble(PossibleCauseDTO::score).reversed()
        .thenComparing(Comparator.comparingInt(PossibleCauseDTO::matchedCount).reversed())
        .thenComparing(PossibleCauseDTO::causeText));

    return causes;
  }

  private PossibleCauseDTO score(SymptomCause cause, Set<String> selected) {
    double matchedWeight = 0;
    double totalWeight = 0;
    List<String> matchedCodes = new ArrayList<>();
    List<SymptomFactorDTO> factors = new ArrayList<>();

    for (SymptomFactor factor : cause.getFactors()) {
      double weight = factor.getWeight() == null ? 1 : factor.getWeight();
      totalWeight += weight;
      factors.add(factorMapper.mapToDTO(factor));

      if (selected.contains(factor.getCode())) {
        matchedWeight += weight;
        matchedCodes.add(factor.getCode());
      }
    }

    // A cause with no factors at all cannot be narrowed down, so it sits with the unmatched ones
    // rather than scoring a spurious 1.0 for an empty description.
    double score = totalWeight == 0 ? 0 : matchedWeight / totalWeight;

    List<ConditionDTO> conditions = cause.getConditions().stream()
        .map(conditionMapper::mapToDTO)
        .toList();

    return new PossibleCauseDTO(cause.getCauseText(), cause.getFactorsText(), conditions, factors,
        matchedCodes, score, matchedCodes.size());
  }
}

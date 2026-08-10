package gr.uniwa.unihealth.backend.service.personalization;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;

import java.util.List;

public interface LabelEvaluatorService {

  /**
   * Full label pipeline entry point. Runs all 3 evaluation phases and returns labels sorted by AHP
   * significance score descending.
   */
  List<String> evaluateAndSort(HealthProfileDTO dto);

  List<String> evaluateAndSortOptional(OptionalHealthProfileDTO dto);
}

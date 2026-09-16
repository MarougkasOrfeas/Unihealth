package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.PossibleCauseDTO;
import gr.uniwa.unihealth.backend.dto.SymptomFactorDTO;

import java.util.Collection;
import java.util.List;

/**
 * The advanced search: narrow a symptom down by its details, and see what could be behind it.
 *
 * @author omaro
 */
public interface PossibleCauseService {

  /**
   * The details a reader can tick for a symptom.
   *
   * @param slug The symptom slug.
   * @return The factors, in display order. Empty when the symptom has no causes table.
   */
  List<SymptomFactorDTO> findFactors(String slug);

  /**
   * Ranks the possible causes of a symptom against the details the reader ticked.
   *
   * @param slug        The symptom slug.
   * @param factorCodes The codes of the ticked factors. May be empty, in which case every cause is
   *                    returned unranked.
   * @return The causes, best match first. Causes that matched nothing are included last rather than
   *         dropped, because the publisher lists them and hiding them would mislead.
   */
  List<PossibleCauseDTO> findPossibleCauses(String slug, Collection<String> factorCodes);
}

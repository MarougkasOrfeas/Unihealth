package gr.uniwa.unihealth.backend.dto;

import java.util.List;

/**
 * A ranked possible cause.
 *
 * <p>A computed result rather than a view of a row, so it is a record and carries no audit fields.
 * Everything the ranking used is returned alongside the score: the UI shows which of the reader's
 * ticks matched, so the ordering can be understood rather than taken on trust.
 *
 * @param causeText        the cause as the publisher worded it
 * @param factorsText      the full description the factors were split out of
 * @param conditions       the linked conditions, empty when the publisher named the cause in prose
 * @param factors          every factor this cause is described by
 * @param matchedFactorCodes the subset the reader ticked
 * @param score            matched weight over total weight, 0 to 1
 * @param matchedCount     how many factors matched, used to break ties on score
 */
public record PossibleCauseDTO(String causeText, String factorsText, List<ConditionDTO> conditions,
                               List<SymptomFactorDTO> factors, List<String> matchedFactorCodes,
                               double score, int matchedCount) {

}

package gr.uniwa.unihealth.backend.service.personalization;

import gr.uniwa.unihealth.backend.dto.LabelUsageDTO;

import java.util.List;

/**
 * Measured engagement per profiling label, used to learn which health themes a user actually reads
 * rather than only which ones their form answers imply.
 *
 * Every method is scoped to a single user id, resolved by the controller from the authenticated
 * principal. Collection is gated on that user's consent, re-checked here rather than trusted from
 * the client: a check that only runs in the browser is not a control.
 */
public interface UserLabelMetricService {

  /**
   * Adds the submitted deltas to the user's running totals.
   *
   * Silently does nothing when the user has not consented. Submitted codes are intersected with
   * the labels the user actually has, so a client cannot invent engagement with labels that are
   * not in its profile, and absurd durations are clamped.
   */
  void record(String username, List<LabelUsageDTO> usage);

  /** What is currently held for this user, so they can see it. */
  List<LabelUsageDTO> findForUser(String userId);

  /** Removes everything held for this user. Called on withdrawal of consent. */
  void deleteForUser(String userId);
}

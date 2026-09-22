package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.UserSurveyDTO;

/**
 * The «Έρευνα UniHealth» survey response of one student.
 *
 * <p>Deliberately not a {@code BaseService}: that contract is id-addressed and admin-shaped, which
 * is exactly wrong for a resource where the only row anybody may touch is their own.
 */
public interface UserSurveyService {

  /**
   * The caller's answers, or an empty response if they have not answered yet.
   *
   * <p>Never throws for a missing response — an unanswered survey is the normal starting state, and
   * the form has to render blank rather than showing an error.
   */
  UserSurveyDTO findForUser(String username);

  /** Stores the caller's answers, replacing any previous response. */
  void saveForUser(String username, UserSurveyDTO dto);
}

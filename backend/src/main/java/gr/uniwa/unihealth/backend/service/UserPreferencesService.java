package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;

/**
 * Reads and updates the email preferences of the logged-in user.
 */
public interface UserPreferencesService {

  /**
   * @return the preferences of the currently logged-in user.
   */
  UserPreferencesDTO findMyPreferences();

  /**
   * Saves the preferences of the currently logged-in user.
   *
   * <p>Sends the goodbye email when, and only when, the newsletter goes from subscribed to
   * unsubscribed, so re-toggling the switch does not send it again.
   *
   * @param dto the requested preferences.
   * @return the saved preferences.
   */
  UserPreferencesDTO updateMyPreferences(UserPreferencesDTO dto);
}

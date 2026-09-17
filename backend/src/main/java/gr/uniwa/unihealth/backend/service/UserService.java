package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;

/**
 * Service interface for the application users.
 *
 * @author omaro
 */
public interface UserService extends BaseUpdatableService<UserDTO> {

  /**
   * Updates status and last login data of user.
   *
   * @param username the username of the user who just logged in.
   */
  void justLoggedIn(String username);

  /**
   * Disables or enables a previously disabled user.
   *
   * @param id                         the id of the user who shall be disabled from accessing the
   *                                   system.
   * @param userStatus                 the status to set according to {@link UserStatus}.
   * @param deactivatedDueToInactivity whether the user is deactivated due to prolonged inactivity.
   * @return the new status of the user.
   */
  UserStatus setUserStatus(String id, UserStatus userStatus, boolean deactivatedDueToInactivity);

  /**
   * Disables or enables a previously disabled user.
   *
   * @param id                         the id of the user who shall be disabled from accessing the
   *                                   system.
   * @param userStatus                 the status to set according to {@link UserStatus}.
   * @param deactivatedDueToInactivity whether the user is deactivated due to prolonged inactivity.
   * @param reason                     the reason for either reactivation or deactivation of the
   *                                   user.
   */
  UserStatus setUserStatus(String id, UserStatus userStatus, boolean deactivatedDueToInactivity,
      String reason);

  /**
   * Returns a suggestion for a username based on specific suggestion rules
   *
   * @param firstname The firstname of the User
   * @param lastname  The lastname of the User
   * @return A suggestion for the username
   */
  String suggestUsername(String firstname, String lastname);

  /**
   * Checks whether a given username already exists in the database for a user
   *
   * @param username The username to check
   * @return a boolean denoting whether the username already exists or not (true/false)
   */
  boolean checkUsernameExists(String username);

  /**
   * Checks whether a given email already exists in the database for a user
   *
   * @param email The email to check
   * @return a boolean denoting whether the email already exists or not (true/false)
   */
  boolean checkEmailExists(String email);
}

package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.dto.UserDTO;

/**
 * Service interface for reading application user data.
 *
 * @author omaro
 */
public interface UserReaderService extends BaseReaderService<UserDTO> {

  /**
   * Finds the details of the currently logged in user.
   *
   * @return the details of the currently logged in user
   */
  UserDTO findLoggedInUser();

  boolean isHealthProfileCompleted();

  /**
   * Whether deactivating or deleting this user would leave the application with no administrator
   * who can still sign in.
   *
   * <p>True when the user has the {@code ADMIN} role and no <em>other</em> active administrator
   * exists. The subject's own status is not considered, so a deactivated administrator is still
   * protected from deletion while they are the only way back into the administration screens.
   *
   * @param userId id of the user about to be deactivated or deleted.
   * @return true if this is the last administrator.
   */
  boolean isLastAdmin(String userId);
}

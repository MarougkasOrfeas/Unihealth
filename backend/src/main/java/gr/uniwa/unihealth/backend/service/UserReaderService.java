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
}

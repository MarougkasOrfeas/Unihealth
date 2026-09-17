package gr.uniwa.unihealth.backend.service;

/**
 * Service interface to deactivate inactive {@link gr.uniwa.unihealth.backend.model.User}s.
 *
 * @author omaro
 */
public interface UserInactiveDeactivationService {
  int deactivateInactiveUsers();
}

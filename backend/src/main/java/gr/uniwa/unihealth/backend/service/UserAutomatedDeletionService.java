package gr.uniwa.unihealth.backend.service;

/**
 * Service interface to delete {@link gr.uniwa.unihealth.backend.model.User} who remain unverified for 21
 * days since account creation or reactivation.
 *
 * @author omaro
 */
public interface UserAutomatedDeletionService {
  int deleteAfterRegistrationExpiry();
}

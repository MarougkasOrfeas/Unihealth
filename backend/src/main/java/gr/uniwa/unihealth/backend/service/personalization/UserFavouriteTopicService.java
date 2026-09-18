package gr.uniwa.unihealth.backend.service.personalization;

import java.util.List;

/**
 * Manages the health topics a user has marked as favourites. Every method is scoped to a single
 * user id, which the controller resolves from the authenticated principal and never from the
 * request, so one user can never read or change another's favourites.
 */
public interface UserFavouriteTopicService {

  /** The topic ids this user has favourited, oldest first. */
  List<String> findForUser(String userId);

  /** Adds a favourite. A no-op when it is already there, so a double click is not an error. */
  void add(String userId, String topicId);

  /** Removes a favourite. A no-op when it was not there. */
  void remove(String userId, String topicId);
}

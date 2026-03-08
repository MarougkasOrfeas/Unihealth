package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User}.
 *
 * @author omaro
 */
@Repository
public interface UserRepository extends BaseRepository<User> {

  /**
   * Finds a user by their unique username. Wrapped in Optional to handle "not found" cases safely.
   */
  Optional<User> findByUsername(String username);

  /**
   * Find Users by their {@link UserStatus}.
   *
   * @return a {@link List} of {@link User} entity objects with the given {@link UserStatus}.
   */
  List<User> findByStatus(UserStatus userStatus);
}

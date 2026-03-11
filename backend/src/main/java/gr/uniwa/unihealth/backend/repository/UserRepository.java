package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.DeactivationMode;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
   * Find Users by their {@link UserStatus} and the last login before the given
   * {@link LocalDateTime}.
   *
   * @param status          the {@link UserStatus} of the users to find
   * @param lastLoginBefore the reference date to look for the last performed login.
   * @param emailSent       whether an Email has already been sent to inform the user about
   *                        prolonged inactivity.
   * @return a {@link List} of {@link User} entity objects that match the criteria.
   */
  List<User> findByStatusAndLastLoginBeforeAndEmailSentNoLoginSince(UserStatus status,
      LocalDateTime lastLoginBefore, boolean emailSent);

  /**
   * Find Users by their {@link UserStatus}.
   *
   * @return a {@link List} of {@link User} entity objects with the given {@link UserStatus}.
   */
  List<User> findByStatus(UserStatus userStatus);

  /**
   * Find Users by their {@link DeactivationMode} and a scheduled deactivation date.
   *
   * @param deactivationMode the {@link DeactivationMode} users might have assigned.
   * @param deactivateAfter  the date (if applicable) at which users are scheduled to be
   *                         deactivated.
   * @return a {@link List} of {@link User} entity objects that match the criteria.
   */
  List<User> findByDeactivationModeAndDeactivateAfterLessThanEqual(
      DeactivationMode deactivationMode, LocalDate deactivateAfter);


  /**
   * Find Users whose automatically set deactivation date is previous or equal to the given date.
   *
   * @param deactivateOn the reference date for deactivation.
   * @return A {@link List} of {@link User}s matching the criteria given by the argument list.
   */
  List<User> findByDeactivateOnLessThanEqual(LocalDateTime deactivateOn);


  @Query("""
        select u.username as username
        from User u
        where lower(u.username) like  lower(concat(:prefix, '%'))
      """)
  List<String> findUsernameStartsWith(String prefix);

  List<User> findByDepartmentIdIn(List<String> departmentIds);
}

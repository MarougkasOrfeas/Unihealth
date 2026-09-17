package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.DeactivationMode;
import gr.uniwa.unihealth.backend.model.enums.UserRoles;
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

  Optional<User> findByEmail(String email);

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
  List<User> findByStatusAndLastLoginIsNull(UserStatus userStatus);


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

  Optional<Boolean> findHealthProfileCompletedByUsername(String username);

  /**
   * Finds users by role and status, e.g. every administrator who can currently be reached.
   *
   * @param role   the {@link UserRoles} to match.
   * @param status the {@link UserStatus} to match.
   * @return a {@link List} of matching {@link User} entity objects.
   */
  List<User> findByRoleAndStatus(UserRoles role, UserStatus status);

  /**
   * Counts users with the given role and status, excluding one. Used to answer "is this the last
   * administrator?" without loading the other administrators.
   *
   * @param role         the {@link UserRoles} to match.
   * @param status       the {@link UserStatus} to match.
   * @param excludedUser id of the user to leave out of the count.
   * @return the number of other users matching the criteria.
   */
  long countByRoleAndStatusAndIdNot(UserRoles role, UserStatus status, String excludedUser);

  /**
   * Candidates for the optional-health-profile reminder.
   *
   * <p>Filters everything that can be expressed in SQL; whether the profile is actually incomplete
   * is decided in the service, since it depends on which fields count as applicable.
   *
   * @param status           only active accounts are nudged.
   * @param maxReminders     the lifetime cap on reminders per user.
   * @param sentBefore       the newest "last reminder" timestamp still eligible, i.e. now minus the
   *                         minimum gap between reminders.
   * @param createdBefore    accounts newer than this are left alone for a few days.
   * @return the users worth examining.
   */
  @Query("""
        select u from User u
        where u.status = :status
          and u.notificationsEnabled = true
          and u.optionalFormRemindersSent < :maxReminders
          and u.lastLogin is not null
          and u.createdOn < :createdBefore
          and (u.optionalFormReminderLastSentOn is null
               or u.optionalFormReminderLastSentOn < :sentBefore)
      """)
  List<User> findRemindableForOptionalForm(UserStatus status, int maxReminders,
      LocalDateTime sentBefore, LocalDateTime createdBefore);
}

package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.DeactivationMode;
import gr.uniwa.unihealth.backend.model.enums.UserRoles;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.service.UserDatedDeactivationService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.UserService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation for {@link UserDatedDeactivationService}
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserDatedDeactivationServiceImpl implements UserDatedDeactivationService {

  private final UserRepository userRepository;

  private final UserService userService;
  private final UserReaderService userReaderService;

  private final EmailNotificationService emailNotificationService;

  @Value("${unihealth.app.www.url}")
  private String appUrl;

  @Override
  public int deactivateExpiredUsers() {
    List<User> users = userRepository.findByDeactivationModeAndDeactivateAfterLessThanEqual(
        DeactivationMode.SCHEDULED, LocalDate.now());

    if (CollectionUtils.isEmpty(users)) {
      log.info("No users to deactivate");
      return 0;
    }

    int deactivated = 0;
    for (User user : users) {
      // Locking out the only administrator would leave nobody able to reach the administration
      // screens, so the schedule is skipped and the remaining administrators are told instead.
      if (userReaderService.isLastAdmin(user.getId())) {
        log.warn("User {} is the last administrator and was not deactivated", user.getId());
        notifyAdministrators(user);
        continue;
      }
      userService.setUserStatus(user.getId(), UserStatus.DEACTIVATED, false);
      deactivated++;
    }

    return deactivated;
  }

  /** Tells the other administrators that a scheduled deactivation was skipped. */
  private void notifyAdministrators(User affectedUser) {
    List<String> recipients = userRepository
        .findByRoleAndStatus(UserRoles.ADMIN, UserStatus.ACTIVE).stream()
        .filter(admin -> !admin.getId().equalsIgnoreCase(affectedUser.getId()))
        .map(User::getEmail)
        .toList();

    if (recipients.isEmpty()) {
      log.warn("No other active administrator to notify about user {}", affectedUser.getId());
      return;
    }

    emailNotificationService.sendEmailNotification(recipients, "el",
        EmailNotificationType.ADMIN_ACCOUNT_DATED, appUrl);
  }
}

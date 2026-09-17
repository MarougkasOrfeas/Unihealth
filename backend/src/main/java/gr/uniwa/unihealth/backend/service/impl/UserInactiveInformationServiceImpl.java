package gr.uniwa.unihealth.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.service.UserInactiveInformationService;
import gr.uniwa.unihealth.backend.service.util.DateUtils;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserInactiveInformationServiceImpl implements UserInactiveInformationService {
  private final UserRepository userRepository;

  private final EmailNotificationService emailNotificationService;

  @Value("${unihealth.app.jobs.users.inform.inactivity.days}")
  private int daysSinceLastLogin;

  @Value("${unihealth.app.www.url}")
  private String appUrl;

  @Override
  public int informInactiveUsers() {
    LocalDateTime lastLoginBefore = LocalDateTime.now().minusDays(daysSinceLastLogin);
    List<User> users =
        userRepository.findByStatusAndLastLoginBeforeAndEmailSentNoLoginSince(UserStatus.ACTIVE,
            lastLoginBefore, false);

    if (CollectionUtils.isEmpty(users)) {
      log.info("No users with prolonged inactivity to inform via Email.");
      return 0;
    }

    for (User user : users) {
      String dueDateForDeactivation = LocalDate.now().plusDays(30).format(DateUtils.DATE_FORMATTER);
      emailNotificationService.sendEmailNotification(List.of(user.getEmail()),
          user.getLanguage().getLocale(), EmailNotificationType.ACCOUNT_ABOUT_TO_EXPIRE,
          dueDateForDeactivation, appUrl);

      user.setEmailSentNoLoginSince(true);
      userRepository.save(user);
    }
    return users.size();
  }
}

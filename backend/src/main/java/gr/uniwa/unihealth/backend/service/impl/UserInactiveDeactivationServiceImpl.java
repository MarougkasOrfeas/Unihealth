package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.UserInactiveDeactivationService;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserInactiveDeactivationServiceImpl implements UserInactiveDeactivationService {

  @Value("${unihealth.app.jobs.users.deactivate.inactivity.days}")
  private int daysSinceLastLogin;

  private final UserRepository userRepository;

  private final UserService userService;

  private final UserReaderService userReaderService;

  @Override
  public int deactivateInactiveUsers() {
    LocalDateTime lastLoginBefore = LocalDateTime.now().minusDays(daysSinceLastLogin);

    List<User> inactiveUsers =
        userRepository.findByStatusAndLastLoginBeforeAndEmailSentNoLoginSince(UserStatus.ACTIVE,
            lastLoginBefore, true);
    List<User> temporarilyActivatedUsers =
        userRepository.findByDeactivateOnLessThanEqual(LocalDateTime.now());

    inactiveUsers.addAll(temporarilyActivatedUsers);

    if (CollectionUtils.isEmpty(inactiveUsers)) {
      log.info("No users to deactivate");
      return 0;
    }

    int deactivated = 0;
    for (User user : inactiveUsers) {
      // Checked here rather than letting setUserStatus throw, so one protected account does not
      // abort the rest of the batch.
      if (userReaderService.isLastAdmin(user.getId())) {
        log.warn("User {} is the last administrator and was not deactivated for inactivity",
            user.getId());
        continue;
      }
      userService.setUserStatus(user.getId(), UserStatus.DEACTIVATED, true);
      deactivated++;
    }

    return deactivated;
  }
}

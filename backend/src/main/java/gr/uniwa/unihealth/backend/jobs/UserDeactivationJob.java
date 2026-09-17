package gr.uniwa.unihealth.backend.jobs;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.service.UserAutomatedDeletionService;
import gr.uniwa.unihealth.backend.service.UserDatedDeactivationService;
import gr.uniwa.unihealth.backend.service.UserInactiveDeactivationService;
import gr.uniwa.unihealth.backend.service.UserInactiveInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeactivationJob {

  private final UserDatedDeactivationService userDatedDeactivationService;
  private final UserInactiveDeactivationService userInactiveDeactivationService;
  private final UserInactiveInformationService userInactiveInformationService;
  private final UserAutomatedDeletionService userAutomatedDeletionService;
  private final TenantContext tenantContext;

  @Scheduled(cron = "${unihealth.app.jobs.users.deactivate.cron}")
  public void scanAndDeactivate() {
    tenantContext.runForEachTenant(tenantId -> {
      int deactivateExpiredUsers = userDatedDeactivationService.deactivateExpiredUsers();
      log.info("{} users with expired accounts were identified and deactivated in tenant {}",
          deactivateExpiredUsers, tenantId);

      int inactiveDeactivationUsers = userInactiveDeactivationService.deactivateInactiveUsers();
      log.info(
          "{} users identified who've been inactive for a prolonged (configured) period and were deactivated in tenant {}",
          inactiveDeactivationUsers, tenantId);

      int inactiveInformedUsers = userInactiveInformationService.informInactiveUsers();
      log.info("{} users have been informed about an imminent deactivation of their accounts.",
          inactiveInformedUsers);

      int deletedUsers = userAutomatedDeletionService.deleteAfterRegistrationExpiry();
      log.info("{} users have been deleted because of exceeded registration expiry date.",
          deletedUsers);
    }, "UserDeactivationJob.scanAndDeactivate");

  }
}

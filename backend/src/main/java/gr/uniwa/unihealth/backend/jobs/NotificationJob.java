package gr.uniwa.unihealth.backend.jobs;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.service.NewsDigestService;
import gr.uniwa.unihealth.backend.service.OptionalFormReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The optional, preference-governed emails.
 *
 * <p>Both run on a schedule rather than off a user action: the reminder cannot hang off logout
 * because logout is handled entirely in the SPA and never reaches the backend, and the digest is
 * periodic by nature.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationJob {

  private final OptionalFormReminderService optionalFormReminderService;
  private final NewsDigestService newsDigestService;
  private final TenantContext tenantContext;

  /** Runs daily; the service decides who is actually due, so a daily pass is cheap. */
  @Scheduled(cron = "${unihealth.app.jobs.notifications.optional-form-reminder.cron}")
  public void remindIncompleteOptionalProfiles() {
    tenantContext.runForEachTenant(tenantId -> {
      int sent = optionalFormReminderService.remindIncompleteOptionalProfiles();
      log.info("{} optional health profile reminders sent in tenant {}", sent, tenantId);
    }, "NotificationJob.remindIncompleteOptionalProfiles");
  }

  @Scheduled(cron = "${unihealth.app.jobs.notifications.news-digest.cron}")
  public void sendNewsDigest() {
    tenantContext.runForEachTenant(tenantId -> {
      int sent = newsDigestService.sendDigest();
      log.info("{} health news digests sent in tenant {}", sent, tenantId);
    }, "NotificationJob.sendNewsDigest");
  }
}

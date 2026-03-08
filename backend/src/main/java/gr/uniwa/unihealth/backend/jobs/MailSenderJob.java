package gr.uniwa.unihealth.backend.jobs;

import com.eurodyn.qlack.fuse.mailing.monitor.MailQueueMonitor;
import com.eurodyn.qlack.fuse.mailing.service.MailService;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Jobs related to mailin functionality.
 *
 * @author omaro
 */
@Component
@RequiredArgsConstructor
public class MailSenderJob {

  private final MailQueueMonitor mailQueueMonitor;
  private final MailService mailService;
  private final TenantContext tenantContext;

  /**
   * Checks the mail queue and sends any queued mails. Scheduled to run at intervals.
   */
  @Scheduled(cron = "${unihealth.app.jobs.mail.send.cron}")
  public void checkAndSendQueued() {
    tenantContext.runForEachTenant(tenantId -> mailQueueMonitor.checkAndSendQueued(),
        "MailSenderJob.checkAndSendQueued");
  }

  /**
   * Cleans up expired mails from the queue. Scheduled to run at intervals.
   */
  @Scheduled(cron = "${unihealth.app.jobs.mail.cleanup.cron}")
  public void cleanupExpired() {
    tenantContext.runForEachTenant(tenantId -> mailService.cleanupExpired(),
        "MailSenderJob.cleanupExpired");
  }
}

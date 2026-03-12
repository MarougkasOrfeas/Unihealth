package gr.uniwa.unihealth.backend.jobs;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.service.RssFeedReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssFeedJob {

  private final RssFeedReaderService rssFeedReaderService;
  private final TenantContext tenantContext;

  /**
   * Refreshes RSS feeds every day at 08:00.
   */
  @Scheduled(cron = "${unihealth.app.rss.jobs.refresh.cron}")
  public void refreshFeeds() {
    tenantContext.runForEachTenant(tenantId -> {
      log.info("Refreshing RSS feeds for tenant [{}].", tenantId);
      rssFeedReaderService.refreshFeeds();
    }, "RssFeedJob.refreshFeeds");
  }
}

package gr.uniwa.unihealth.backend.service.extensions;

import com.eurodyn.qlack.fuse.mailing.mapper.EmailMapper;
import com.eurodyn.qlack.fuse.mailing.monitor.MailQueueMonitor;
import com.eurodyn.qlack.fuse.mailing.monitor.MailQueueSender;
import com.eurodyn.qlack.fuse.mailing.repository.DistributionListRepository;
import com.eurodyn.qlack.fuse.mailing.repository.EmailRepository;
import com.eurodyn.qlack.fuse.mailing.util.MailingProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension of the QLACK MailQueueMonitor.
 */
@Service
@Transactional
public class MailQueueMonitorExtension extends MailQueueMonitor {

  public MailQueueMonitorExtension(MailQueueSender mailQueueSender,
      MailingProperties mailingProperties, EmailRepository emailRepository,
      DistributionListRepository distributionListRepository, EmailMapper emailMapper) {
    super(mailQueueSender, mailingProperties, emailRepository, distributionListRepository,
        emailMapper);
  }

  @Override
  @Scheduled(cron = "-")
  public void checkAndSendQueued() {
    super.checkAndSendQueued();
  }
}

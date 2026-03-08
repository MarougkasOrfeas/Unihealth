package gr.uniwa.unihealth.backend.service.extensions;

import com.eurodyn.qlack.fuse.mailing.mapper.AttachmentMapper;
import com.eurodyn.qlack.fuse.mailing.mapper.EmailMapper;
import com.eurodyn.qlack.fuse.mailing.monitor.MailQueueMonitor;
import com.eurodyn.qlack.fuse.mailing.repository.AttachmentRepository;
import com.eurodyn.qlack.fuse.mailing.repository.EmailRepository;
import com.eurodyn.qlack.fuse.mailing.service.MailService;
import com.eurodyn.qlack.fuse.mailing.validators.EmailValidator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension of the QLACK MailService.
 */
@Service
@Transactional
public class MailServiceExtension extends MailService {

  public MailServiceExtension(MailQueueMonitor mailQueueMonitor, EmailMapper emailMapper,
      EmailRepository emailRepository, AttachmentMapper attachmentMapper,
      AttachmentRepository attachmentRepository, EmailValidator emailValidator) {
    super(mailQueueMonitor, emailMapper, emailRepository, attachmentMapper, attachmentRepository,
        emailValidator);
  }

  @Scheduled(cron = "-")
  public void cleanupExpired() {
    super.cleanupExpired();
  }
}

package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.fuse.mailing.dto.EmailDTO;
import com.eurodyn.qlack.fuse.mailing.dto.EmailDTO.EMAIL_TYPE;
import com.eurodyn.qlack.fuse.mailing.service.MailService;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationUtils;
import gr.uniwa.unihealth.backend.utils.email.EmailSubjectBodyPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;

/**
 * Service for sending email notifications to users. Handles localization based on user's preferred
 * language.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

  @Value("${spring.mail.from}")
  private String mailFrom;

  @Value("${spring.mail.from-display-name}")
  private String mailFromName;

  private final MailService mailService;

  @Override
  public void sendEmailNotification(List<String> emailAddress, String locale,
      EmailNotificationType emailNotificationType, Object... emailBodyVars) {
    EmailSubjectBodyPair emailSubjectBodyPair =
        EmailNotificationUtils.getSubjectBodyPairByNotificationTypeAndLanguage(
            emailNotificationType, locale);
    String subject = emailSubjectBodyPair.subject();
    String body = emailSubjectBodyPair.body();

    if (emailBodyVars != null && emailBodyVars.length > 0) {
      body = MessageFormat.format(body, emailBodyVars);
    }

    EmailDTO emailDTO = new EmailDTO();
    emailDTO.setFromEmail(mailFromName + " <" + mailFrom + ">");
    emailDTO.setToEmails(emailAddress);
    emailDTO.setSubject(subject);
    emailDTO.setBody(body);
    emailDTO.setEmailType(EMAIL_TYPE.HTML);

    mailService.queueEmail(emailDTO);

    log.info("{} => Sending Email to address: {}", emailNotificationType, emailAddress);
  }
}

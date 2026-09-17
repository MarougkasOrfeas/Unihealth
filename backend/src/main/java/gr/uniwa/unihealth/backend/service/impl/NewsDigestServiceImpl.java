package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.model.RssFeed;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.RssFeedRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.service.EmailPreferenceService;
import gr.uniwa.unihealth.backend.service.NewsDigestService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation for {@link NewsDigestService}.
 *
 * <p>This is what gives the "Unsubscribe from News Feeds" preference something to govern: without
 * a digest, the flag would be a column nothing reads.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsDigestServiceImpl implements NewsDigestService {

  /** Enough to be worth reading, few enough to stay skimmable. */
  private static final int ITEMS_PER_DIGEST = 5;

  private final UserRepository userRepository;
  private final RssFeedRepository rssFeedRepository;
  private final EmailNotificationService emailNotificationService;
  private final EmailPreferenceService emailPreferenceService;

  @Value("${unihealth.app.www.url}")
  private String appUrl;

  @Override
  public int sendDigest() {
    List<RssFeed> items = rssFeedRepository
        .findAllByOrderByPublishedDateDesc(PageRequest.of(0, ITEMS_PER_DIGEST))
        .getContent();

    if (CollectionUtils.isEmpty(items)) {
      log.info("No health news to send; skipping the digest.");
      return 0;
    }

    String itemsHtml = renderItems(items);

    int sent = 0;
    for (User user : userRepository.findByStatus(UserStatus.ACTIVE)) {
      if (!emailPreferenceService.mayReceive(user, EmailNotificationType.NEWS_DIGEST)) {
        continue;
      }

      emailNotificationService.sendEmailNotification(List.of(user.getEmail()), localeOf(user),
          EmailNotificationType.NEWS_DIGEST, itemsHtml, appUrl);
      sent++;
    }

    log.info("Queued the health news digest for {} users.", sent);
    return sent;
  }

  private String renderItems(List<RssFeed> items) {
    StringBuilder html = new StringBuilder("<ul>");
    for (RssFeed item : items) {
      html.append("<li><a href=\"").append(escape(item.getLink())).append("\">")
          .append(escape(item.getTitle())).append("</a>");
      if (item.getSourceName() != null && !item.getSourceName().isBlank()) {
        html.append(" &mdash; ").append(escape(item.getSourceName()));
      }
      html.append("</li>");
    }
    return html.append("</ul>").toString();
  }

  /**
   * Feed content is third-party text going into an HTML email, so it is escaped rather than
   * trusted. Curly braces are dropped because the result is handed to {@code MessageFormat}, which
   * would otherwise read them as argument placeholders.
   */
  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
        .replace("{", "")
        .replace("}", "");
  }

  private String localeOf(User user) {
    return user.getLanguage() != null ? user.getLanguage().getLocale() : "el";
  }
}

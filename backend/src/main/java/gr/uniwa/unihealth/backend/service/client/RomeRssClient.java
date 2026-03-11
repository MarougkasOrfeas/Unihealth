package gr.uniwa.unihealth.backend.service.client;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import gr.uniwa.unihealth.backend.config.properties.RssSourceProperties;
import gr.uniwa.unihealth.backend.model.RssFeed;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RomeRssClient implements RssClient {

  @Override
  public List<RssFeed> fetch(RssSourceProperties.Source source) {
    try (XmlReader reader = new XmlReader(new URL(source.getUrl()))) {
      SyndFeed syndFeed = new SyndFeedInput().build(reader);

      return syndFeed.getEntries().stream().map(entry -> mapEntry(entry, source)).toList();

    } catch (Exception e) {
      throw new RuntimeException("Failed to read RSS feed from " + source.getUrl(), e);
    }
  }

  private RssFeed mapEntry(SyndEntry entry, RssSourceProperties.Source source) {
    RssFeed rssFeed = new RssFeed();
    rssFeed.setTitle(entry.getTitle());
    rssFeed.setSummary(extractSummary(entry));
    rssFeed.setLink(entry.getLink());
    rssFeed.setPublishedDate(toLocalDateTime(entry.getPublishedDate()));
    rssFeed.setSourceName(source.getName());
    rssFeed.setImageUrl(extractImageUrl(entry));
    return rssFeed;
  }

  private LocalDateTime toLocalDateTime(Date date) {
    if (date == null) {
      return null;
    }
    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  private String extractImageUrl(SyndEntry entry) {
    if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
      String enclosureUrl = entry.getEnclosures().get(0).getUrl();
      if (enclosureUrl != null && !enclosureUrl.isBlank()) {
        return normalizeUrl(enclosureUrl);
      }
    }

    String imageHtml = extractImageHtml(entry);
    if (imageHtml != null && !imageHtml.isBlank()) {
      Document doc = Jsoup.parse(imageHtml);
      Element firstRealImage = doc.selectFirst("img[src]:not(.wp-smiley)");
      if (firstRealImage != null) {
        return normalizeUrl(firstRealImage.attr("src"));
      }
    }

    return null;
  }

  private String extractSummary(SyndEntry entry) {
    String summaryHtml = extractSummaryHtml(entry);
    if (summaryHtml == null || summaryHtml.isBlank()) {
      return null;
    }

    Document doc = Jsoup.parse(summaryHtml);

    doc.select("img").remove();
    doc.select(".read-more").remove();

    String text = doc.text();

    text = text.replaceAll("The post .*? appeared first on .*?\\.", "").trim();
    text = text.replaceAll("\\s+", " ").trim();

    return text.isBlank() ? null : text;
  }

  private String extractSummaryHtml(SyndEntry entry) {
    if (entry.getDescription() != null && entry.getDescription()
        .getValue() != null && !entry.getDescription().getValue().isBlank()) {
      return entry.getDescription().getValue();
    }

    List<SyndContent> contents = entry.getContents();
    if (contents != null && !contents.isEmpty() && contents.get(0)
        .getValue() != null && !contents.get(0).getValue().isBlank()) {
      return contents.get(0).getValue();
    }

    return null;
  }

  private String extractImageHtml(SyndEntry entry) {
    List<SyndContent> contents = entry.getContents();
    if (contents != null && !contents.isEmpty() && contents.get(0)
        .getValue() != null && !contents.get(0).getValue().isBlank()) {
      return contents.get(0).getValue();
    }

    if (entry.getDescription() != null && entry.getDescription()
        .getValue() != null && !entry.getDescription().getValue().isBlank()) {
      return entry.getDescription().getValue();
    }

    return null;
  }

  private String normalizeUrl(String url) {
    if (url == null || url.isBlank()) {
      return url;
    }
    return url.replaceFirst("^http://", "https://");
  }
}

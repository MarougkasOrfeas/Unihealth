package gr.uniwa.unihealth.backend.service.ai.external.source;

import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.external.ExternalCandidate;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedHealthSource;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedSourceFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shared behaviour for authorities that publish a dated feed.
 *
 * <p>All three of the sources consulted expose one, which makes discovery a matter of reading XML
 * rather than scraping a layout — considerably more stable, since a site redesign changes the page
 * but rarely the feed. Subclasses supply only what differs: the feed URL, the domain, the name and
 * the authority tier.
 *
 * <p><b>Undated items are dropped, never dated by guesswork.</b> The whole purpose of consulting
 * these sites is to answer "is there anything recent", and an item of unknown age cannot
 * contribute to that answer — treating a missing date as "now" would let an archive page present
 * itself as today's news.
 *
 * <p>Every failure path returns an empty list. These are public websites that will be restructured
 * without notice, and a student's health question must not depend on one of them being reachable
 * or on its markup being unchanged.
 *
 * @author omaro
 */
@Slf4j
@RequiredArgsConstructor
public abstract class FeedBackedHealthSource implements TrustedHealthSource {

  /** How many index entries are considered before ranking. More is just latency. */
  private static final int MAX_ITEMS = 25;

  private final TrustedSourceFetcher fetcher;
  private final AiProperties aiProperties;

  /** The feed to read. Must be on {@link #domain()}; the fetcher re-checks. */
  protected abstract String feedUrl();

  @Override
  public List<ExternalCandidate> recent(LocalDate notBefore) {
    // The index is the one thing here that changes, so it takes the short ttl. Half an hour is far
    // fresher than any of these authorities publishes, and it is what keeps a burst of students
    // asking the same question from becoming a burst of requests.
    Optional<String> xml =
        fetcher.fetch(feedUrl(), domain(), aiProperties.getExternal().getIndexCacheTtl());

    if (xml.isEmpty()) {
      log.debug("No index available from [{}].", sourceName());
      return List.of();
    }

    try {
      return parse(xml.get(), notBefore);
    } catch (Exception e) {
      // A parser failure means the feed changed shape. That is a reason to fix this adapter, not a
      // reason for the turn to fail.
      log.warn("Could not parse the index from [{}]: {}", sourceName(), e.getMessage());
      return List.of();
    }
  }

  private List<ExternalCandidate> parse(String xml, LocalDate notBefore) {
    Document feed = Jsoup.parse(xml, "", Parser.xmlParser());
    List<ExternalCandidate> candidates = new ArrayList<>();

    // RSS uses <item>, Atom uses <entry>. Handling both means a source switching format does not
    // silently return nothing.
    Elements entries = feed.select("item, entry");

    for (Element entry : entries) {
      candidateOf(entry, notBefore).ifPresent(candidates::add);

      if (candidates.size() >= MAX_ITEMS) {
        break;
      }
    }

    Comparator<ExternalCandidate> byDate = Comparator.comparing(ExternalCandidate::publishedAt);
    candidates.sort(byDate.reversed());

    log.debug("[{}] offered {} candidates published on or after {}.",
        sourceName(), candidates.size(), notBefore);

    return candidates;
  }

  private Optional<ExternalCandidate> candidateOf(Element entry, LocalDate notBefore) {
    LocalDate published = publishedDate(entry).orElse(null);
    if (published == null || published.isBefore(notBefore)) {
      return Optional.empty();
    }

    String title = text(entry, "title");
    String url = link(entry);

    if (title.isBlank() || url.isBlank()) {
      return Optional.empty();
    }

    return Optional.of(ExternalCandidate.discovered(title, url, published, snippet(entry),
        sourceName(), authority()));
  }

  /**
   * Feeds disagree about which element carries the date and in which format, so every plausible
   * combination is tried before the item is dropped.
   */
  private Optional<LocalDate> publishedDate(Element entry) {
    for (String field : List.of("pubDate", "published", "updated", "dc|date", "date")) {
      String raw = text(entry, field);
      if (raw.isBlank()) {
        continue;
      }

      Optional<LocalDate> parsed = parseDate(raw);
      if (parsed.isPresent()) {
        return parsed;
      }
    }
    return Optional.empty();
  }

  private Optional<LocalDate> parseDate(String raw) {
    String value = raw.trim();

    // RFC 1123 for RSS, ISO for Atom. Tried in that order because RSS is the commoner case here.
    for (DateTimeFormatter formatter :
        List.of(DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME)) {
      try {
        return Optional.of(OffsetDateTime.parse(value, formatter).toLocalDate());
      } catch (DateTimeParseException ignored) {
        // Try the next shape.
      }
    }

    try {
      return Optional.of(LocalDate.parse(value.substring(0, Math.min(10, value.length()))));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  /**
   * RSS puts the URL in {@code <link>} text; Atom puts it in a {@code href} attribute. Both appear
   * in practice, so both are read.
   */
  private String link(Element entry) {
    Element linkElement = entry.selectFirst("link");
    if (linkElement == null) {
      return "";
    }

    String href = linkElement.attr("href");
    return href.isBlank() ? linkElement.text().trim() : href.trim();
  }

  /**
   * Feed summaries are HTML. Stripped to text here rather than downstream, so nothing that reaches
   * the prompt or the client ever carries markup.
   */
  private String snippet(Element entry) {
    for (String field : List.of("description", "summary", "content")) {
      String raw = text(entry, field);
      if (!raw.isBlank()) {
        return Jsoup.parse(raw).text().replaceAll("\\s+", " ").trim();
      }
    }
    return "";
  }

  private String text(Element entry, String tag) {
    Element found = entry.selectFirst(tag.toLowerCase(Locale.ROOT));
    return found == null ? "" : found.text().trim();
  }
}

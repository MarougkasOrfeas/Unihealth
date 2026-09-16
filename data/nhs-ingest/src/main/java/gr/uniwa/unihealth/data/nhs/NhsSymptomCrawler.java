package gr.uniwa.unihealth.data.nhs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks the NHS symptoms A-Z and writes a snapshot of it.
 *
 * <p><b>Why this source.</b> The NHS website is Crown Copyright published under the Open Government
 * Licence v3.0, which permits reuse with attribution; the NHS documents scraping as a supported way
 * to obtain it. The pages also carry schema.org JSON-LD, so the section structure is read from a
 * published vocabulary rather than guessed from headings.
 *
 * <p>&#9888; <b>Scope limit that matters.</b> The nhs.uk robots.txt disallows {@code /Conditions/}.
 * Only {@code /symptoms/} is crawled, and the conditions named in the causes tables are stored as
 * outbound links rather than being fetched. Do not "improve" this by following those links -
 * {@link PoliteHttpFetcher} would refuse them anyway, which is the point.
 *
 * @author omaro
 */
@Slf4j
public class NhsSymptomCrawler {

  public static final String SOURCE_CODE = "NHS";

  private static final String INDEX_URL = "https://www.nhs.uk/symptoms/";
  private static final String SYMPTOM_PATH = "/symptoms/";

  private final PoliteHttpFetcher fetcher;
  private final NhsSymptomParser parser;
  private final ObjectMapper objectMapper;
  private final int maxPages;

  public NhsSymptomCrawler(PoliteHttpFetcher fetcher, NhsSymptomParser parser,
      ObjectMapper objectMapper, int maxPages) {
    this.fetcher = fetcher;
    this.parser = parser;
    this.objectMapper = objectMapper;
    this.maxPages = maxPages;
  }

  /**
   * Fetches the A-Z and every symptom page below it.
   *
   * @return the snapshot, or empty when the index could not be read
   */
  public NhsSnapshot crawl() {
    log.info("Reading the NHS symptoms index from {}", INDEX_URL);

    List<String> slugs = fetcher.fetch(INDEX_URL).map(this::readSlugs).orElse(List.of());
    if (slugs.isEmpty()) {
      log.warn("Could not read the NHS symptoms index; nothing to ingest.");
      return null;
    }

    if (maxPages > 0 && slugs.size() > maxPages) {
      log.info("Limiting this run to the configured {} pages.", maxPages);
      slugs = slugs.subList(0, maxPages);
    }

    log.info("Found {} symptom pages; fetching them at one request per second.", slugs.size());

    List<NhsSnapshot.Symptom> symptoms = new ArrayList<>();
    for (String slug : slugs) {
      fetcher.fetch(INDEX_URL + slug + "/")
          .map(html -> parser.parse(html, slug))
          .ifPresent(symptoms::add);
    }

    long withFactors = symptoms.stream().filter(s -> !s.causes().isEmpty()).count();
    long causeRows = symptoms.stream().mapToLong(s -> s.causes().size()).sum();
    log.info("Parsed {} symptoms, {} of which have a causes table ({} rows in total).",
        symptoms.size(), withFactors, causeRows);

    return new NhsSnapshot(source(), symptoms);
  }

  /** Serialises a snapshot so it can be committed and replayed offline. */
  public void write(NhsSnapshot snapshot, Path target) throws IOException {
    Files.createDirectories(target.getParent());
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), snapshot);
    log.info("Wrote the NHS snapshot to {} ({} KB).", target,
        Files.size(target) / 1024);
  }

  private List<String> readSlugs(String html) {
    Set<String> slugs = new LinkedHashSet<>();

    for (Element link : Jsoup.parse(html, INDEX_URL).select("a[href]")) {
      String href = link.attr("href");
      int index = href.indexOf(SYMPTOM_PATH);
      if (index < 0) {
        continue;
      }
      String slug = href.substring(index + SYMPTOM_PATH.length()).replaceAll("[/#?].*$", "");
      if (!slug.isBlank()) {
        slugs.add(slug);
      }
    }

    return List.copyOf(slugs);
  }

  private NhsSnapshot.Source source() {
    return new NhsSnapshot.Source(
        SOURCE_CODE,
        "NHS website",
        INDEX_URL,
        "Open Government Licence v3.0",
        "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
        "Contains public sector information licensed under the Open Government Licence v3.0. "
            + "Symptom content is Crown Copyright, adapted from the NHS website.",
        "https://assets.nhs.uk/nhsuk-cms/images/nhs-attribution.width-510.png",
        LocalDate.now());
  }
}

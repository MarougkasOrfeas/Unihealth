package gr.uniwa.unihealth.data.nhs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

/**
 * Runs the NHS crawl on demand and writes the snapshot that the backend seeds from.
 *
 * <p>Refreshing the corpus is a developer decision, not something a normal backend start should do.
 * To refresh it:
 *
 * <pre>
 * mvn -f data/nhs-ingest/pom.xml exec:java
 * </pre>
 *
 * <p>Then commit the regenerated backend snapshot. The crawl takes about two minutes at the
 * one-request-per-second rate.
 *
 * @author omaro
 */
@Slf4j
@RequiredArgsConstructor
public class NhsIngestRunner {

  private static final String DEFAULT_USER_AGENT =
      "UniHealthBot/1.0 (+https://www.uniwa.gr; thesis project)";
  private static final String DEFAULT_OUTPUT =
      "../../backend/src/main/resources/data/nhs/nhs-symptoms.json";

  private final NhsSymptomCrawler crawler;
  private final Path output;

  public static void main(String[] args) {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    String userAgent = System.getProperty("nhs.ingest.user-agent", DEFAULT_USER_AGENT);
    int maxPages = Integer.getInteger("nhs.ingest.max-pages", 0);
    Path output = Path.of(System.getProperty("nhs.ingest.output", DEFAULT_OUTPUT));

    PoliteHttpFetcher fetcher = new PoliteHttpFetcher(userAgent);
    NhsSymptomParser parser = new NhsSymptomParser(objectMapper);
    NhsSymptomCrawler crawler = new NhsSymptomCrawler(fetcher, parser, objectMapper, maxPages);

    new NhsIngestRunner(crawler, output).run();
  }

  public void run() {

    try {
      NhsSnapshot snapshot = crawler.crawl();
      if (snapshot == null) {
        return;
      }
      crawler.write(snapshot, output);
      log.info("NHS ingestion finished. Commit the snapshot, then restart with ingestion off.");
    } catch (Exception e) {
      log.error("NHS ingestion failed: {}", e.getMessage(), e);
    }
  }
}

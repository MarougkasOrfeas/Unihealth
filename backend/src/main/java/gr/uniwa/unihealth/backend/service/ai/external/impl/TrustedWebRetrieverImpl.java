package gr.uniwa.unihealth.backend.service.ai.external.impl;

import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.AiTurnProgress;
import gr.uniwa.unihealth.backend.service.ai.AiTurnStage;
import gr.uniwa.unihealth.backend.service.ai.external.ExternalCandidate;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedHealthSource;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedSourceFetcher;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedWebRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation for {@link TrustedWebRetriever}.
 *
 * <p>Four steps, under one clock:
 *
 * <ol>
 *   <li><b>Discover in parallel.</b> Every adapter reads its own index at once, because three
 *       sequential fetches would spend the whole budget on the first two.</li>
 *   <li><b>Filter by date.</b> Anything older than the configured window is dropped. "Recent" has
 *       to mean something or the feature is just a slower way to be wrong.</li>
 *   <li><b>Rank with bge-m3.</b> Candidates are embedded and compared against the question. This
 *       is the step that keeps roughly twenty index entries from crowding the NHS passages out of
 *       an 8192-token window.</li>
 *   <li><b>Read the best few.</b> Only the survivors have their page fetched, and only if the
 *       clock allows. A ranked candidate with just its title and snippet is still useful.</li>
 * </ol>
 *
 * <p><b>The budget is a ceiling on the whole thing, not per request.</b> Whatever has arrived when
 * it expires is what gets used. Nothing here throws: a source that is slow, broken, restructured
 * or simply down contributes nothing and the turn continues.
 *
 * <p>These candidates are deliberately <em>not</em> written to the vector store. That store holds
 * vetted, licensed content whose provenance the application controls; mixing live pages into it
 * would make the distinction between the two unrecoverable the moment it mattered.
 *
 * @author omaro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrustedWebRetrieverImpl implements TrustedWebRetriever {

  /** How many candidates have their full page read. Each one is another request on the clock. */
  private static final int MAX_BODIES_FETCHED = 2;

  private final List<TrustedHealthSource> sources;
  private final TrustedSourceFetcher fetcher;
  private final EmbeddingModel embeddingModel;
  private final AiProperties aiProperties;

  @Override
  public List<ExternalCandidate> retrieve(String question, AiTurnProgress progress) {
    Instant deadline = Instant.now().plus(aiProperties.getExternal().getBudget());
    LocalDate notBefore = LocalDate.now().minusDays(aiProperties.getExternal().getMaxAgeDays());

    List<ExternalCandidate> candidates = discover(notBefore, deadline);
    if (candidates.isEmpty()) {
      log.debug("No trusted authority offered anything published since {}.", notBefore);
      return List.of();
    }

    List<ExternalCandidate> ranked = rank(question, candidates);

    return readBodies(ranked, deadline, progress);
  }

  /** Every adapter at once, joined under the remaining budget. */
  private List<ExternalCandidate> discover(LocalDate notBefore, Instant deadline) {
    List<CompletableFuture<List<ExternalCandidate>>> futures = sources.stream()
        .map(source -> CompletableFuture.supplyAsync(() -> safely(source, notBefore)))
        .toList();

    awaitAll(futures, deadline);

    // Deduped by URL: the same WHO item can legitimately be syndicated by ECDC, and citing it
    // twice would waste a slot in a list of three.
    Map<String, ExternalCandidate> byUrl = new LinkedHashMap<>();

    for (CompletableFuture<List<ExternalCandidate>> future : futures) {
      if (!future.isDone() || future.isCompletedExceptionally()) {
        continue;
      }
      for (ExternalCandidate candidate : future.join()) {
        byUrl.putIfAbsent(candidate.url(), candidate);
      }
    }

    return new ArrayList<>(byUrl.values());
  }

  private List<ExternalCandidate> safely(TrustedHealthSource source, LocalDate notBefore) {
    try {
      return source.recent(notBefore);
    } catch (Exception e) {
      // The interface asks implementations not to throw. This is the belt to that braces: one
      // adapter's bug must not take down a health answer.
      log.warn("Source [{}] failed during discovery: {}", source.sourceName(), e.getMessage());
      return List.of();
    }
  }

  /**
   * Cosine against the question, with authority tier and recency breaking ties.
   *
   * <p>Similarity alone would rank a year-old global bulletin above a fresh national notice
   * whenever the wording matched better, which is the opposite of useful for a student in Athens.
   */
  private List<ExternalCandidate> rank(String question, List<ExternalCandidate> candidates) {
    Map<ExternalCandidate, Double> scores = similarities(question, candidates);

    // Built as explicitly typed locals rather than one chained expression. Comparator.thenComparing
    // is overloaded for both Function and Comparator, and a method reference passed into it is a
    // reliable way to produce an inference error that reads as if something else is wrong.
    Comparator<ExternalCandidate> byScore =
        Comparator.comparingDouble(candidate -> scores.getOrDefault(candidate, 0d));
    Comparator<ExternalCandidate> byAuthority =
        Comparator.comparing(ExternalCandidate::authority);
    Comparator<ExternalCandidate> byDate =
        Comparator.comparing(ExternalCandidate::publishedAt);

    return candidates.stream()
        .sorted(byScore.reversed().thenComparing(byAuthority).thenComparing(byDate.reversed()))
        .limit(aiProperties.getExternal().getTopK())
        .toList();
  }

  private Map<ExternalCandidate, Double> similarities(String question,
      List<ExternalCandidate> candidates) {

    Map<ExternalCandidate, Double> scores = new LinkedHashMap<>();

    try {
      float[] questionVector = embeddingModel.embed(question);

      List<String> texts = candidates.stream().map(ExternalCandidate::text).toList();
      List<float[]> vectors = embeddingModel.embed(texts);

      for (int i = 0; i < candidates.size() && i < vectors.size(); i++) {
        scores.put(candidates.get(i), cosine(questionVector, vectors.get(i)));
      }
    } catch (Exception e) {
      // Without Ollama there is no ranking, but the candidates are all recent and all from trusted
      // authorities - falling back to authority and date order is a reasonable second best.
      log.warn("Could not rank external candidates ({}); falling back to recency.", e.getMessage());
    }

    return scores;
  }

  private static double cosine(float[] left, float[] right) {
    if (left == null || right == null || left.length != right.length) {
      return 0d;
    }

    double dot = 0;
    double leftNorm = 0;
    double rightNorm = 0;

    for (int i = 0; i < left.length; i++) {
      dot += (double) left[i] * right[i];
      leftNorm += (double) left[i] * left[i];
      rightNorm += (double) right[i] * right[i];
    }

    if (leftNorm == 0 || rightNorm == 0) {
      return 0d;
    }

    return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
  }

  /**
   * Reads the pages of the best candidates, while the clock allows.
   *
   * <p>A candidate whose page could not be read in time is kept, not dropped: its title, date and
   * snippet came from the authority's own index and are perfectly citable on their own.
   */
  private List<ExternalCandidate> readBodies(List<ExternalCandidate> ranked, Instant deadline,
      AiTurnProgress progress) {

    List<ExternalCandidate> result = new ArrayList<>(ranked.size());
    int fetched = 0;

    if (!ranked.isEmpty()) {
      progress.report(AiTurnStage.READING_SOURCES);
    }

    for (ExternalCandidate candidate : ranked) {
      if (fetched >= MAX_BODIES_FETCHED || Instant.now().isAfter(deadline)) {
        result.add(candidate);
        continue;
      }

      result.add(bodyOf(candidate).map(candidate::withBody).orElse(candidate));
      fetched++;
    }

    return result;
  }

  private Optional<String> bodyOf(ExternalCandidate candidate) {
    return fetcher.fetch(candidate.url(), domainOf(candidate),
            aiProperties.getExternal().getPageCacheTtl())
        .map(this::toPlainText)
        .filter(StringUtils::isNotBlank);
  }

  /**
   * Markup stripped before the text can reach a prompt, and truncated so one verbose page cannot
   * displace the NHS passages it is supposed to supplement.
   */
  private String toPlainText(String html) {
    String text = Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    int cap = aiProperties.getExternal().getMaxPassageChars();

    return text.length() <= cap ? text : text.substring(0, cap);
  }

  /** The owning adapter's domain, so the fetcher can refuse anything that drifted off-site. */
  private String domainOf(ExternalCandidate candidate) {
    return sources.stream()
        .filter(source -> source.sourceName().equals(candidate.sourceName()))
        .map(TrustedHealthSource::domain)
        .findFirst()
        // No adapter claims this candidate, so nothing may be fetched for it. An empty domain
        // fails the fetcher's host check, which is the safe direction.
        .orElse("");
  }

  private void awaitAll(List<CompletableFuture<List<ExternalCandidate>>> futures,
      Instant deadline) {
    long remaining = Duration.between(Instant.now(), deadline).toMillis();

    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .get(Math.max(remaining, 0), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      // Timeout or a failing source. Both are expected and neither is worth a stack trace: the
      // caller reads whichever futures did complete.
      log.debug("External discovery did not finish within the budget.");
    }
  }
}

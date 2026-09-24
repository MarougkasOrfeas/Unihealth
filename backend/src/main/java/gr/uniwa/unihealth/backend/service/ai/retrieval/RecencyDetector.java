package gr.uniwa.unihealth.backend.service.ai.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTextAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides whether a question is asking about something time-varying.
 *
 * <p>This is the signal that separates «τι να κάνω για τον πονόλαιμο;», which the committed NHS
 * snapshot answers perfectly, from «υπάρχει έξαρση κρουσμάτων;», which it structurally cannot
 * answer at all. Only the second is worth a live lookup at a health authority.
 *
 * <p>Deterministic rather than a classifier prompt, for the same reason as {@link
 * gr.uniwa.unihealth.backend.service.ai.safety.CrisisGuard}: asking an 8B model to classify intent
 * costs a whole extra generation - seconds, on this hardware - to answer a question a dictionary
 * answers in microseconds. It also makes the behaviour testable without a model running.
 *
 * <p><b>The errors are not symmetrical, and the dataset is tuned accordingly.</b> A false positive
 * costs one network request and a little latency. A false negative costs nothing at all: the
 * student still gets the grounded local answer, which is a good answer. So the cue list is
 * deliberately specific - «νέα» and «σήμερα» are excluded as too common - rather than broad.
 *
 * @author omaro
 */
@Slf4j
@Component
public class RecencyDetector {

  private static final String RESOURCE = "data/ai/recency-cues.json";

  private final Set<String> bySurface = new HashSet<>();
  private final Set<String> byStem = new HashSet<>();
  private final Set<String> bySkeleton = new HashSet<>();
  private int maxCueWords = 1;

  public RecencyDetector() {
    this(readResource());
  }

  /** Test seam: build from an inline dataset instead of the committed file. */
  public RecencyDetector(JsonNode dataset) {
    index(dataset);
  }

  /**
   * @return true when the question is asking about current or recent events.
   */
  public boolean asksAboutRecent(String question) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(question);
    if (tokens.isEmpty()) {
      return false;
    }

    for (int i = 0; i < tokens.size(); i++) {
      int maxSpan = Math.min(maxCueWords, tokens.size() - i);

      // Longest span first, so "this year" is seen as a phrase rather than as a bare "this".
      for (int n = maxSpan; n >= 1; n--) {
        if (matches(tokens, i, n)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean matches(List<MedicalTextAnalyzer.Token> tokens, int from, int count) {
    if (bySurface.contains(MedicalTextAnalyzer.joinSurfaces(tokens, from, count))
        || byStem.contains(MedicalTextAnalyzer.joinStems(tokens, from, count))) {
      return true;
    }

    // Latin script only: a student writing Greek in Latin letters ("iparxi eksarsi"). Tried last so
    // a real English phrase is never read as transliterated Greek.
    return !tokens.get(from).greek()
        && bySkeleton.contains(MedicalTextAnalyzer.joinSkeletons(tokens, from, count));
  }

  private void index(JsonNode dataset) {
    for (JsonNode cue : dataset.path("cues")) {
      register(cue.path("text").asText());
    }

    if (bySurface.isEmpty()) {
      // Not fatal, unlike the crisis dictionary: with no cues the assistant simply never consults
      // an authority, which costs recency but harms nobody. Degrading beats refusing to start.
      log.warn("Recency cue dictionary is empty; live authority lookups will never trigger.");
      return;
    }

    log.info("Indexed {} recency cues, longest {} words.", bySurface.size(), maxCueWords);
  }

  private void register(String text) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(text);
    if (tokens.isEmpty()) {
      return;
    }

    int words = tokens.size();

    // A one-word cue below the analyzer's floor is not registered. Short stems are traps: the same
    // rule MedicalTermIndex documents for «τόνος» collapsing onto the Greek article.
    if (words == 1 && tokens.getFirst().surface().length() < MedicalTextAnalyzer.MIN_KEY_LENGTH) {
      return;
    }

    maxCueWords = Math.max(maxCueWords, words);

    bySurface.add(MedicalTextAnalyzer.joinSurfaces(tokens, 0, words));
    byStem.add(MedicalTextAnalyzer.joinStems(tokens, 0, words));
    bySkeleton.add(MedicalTextAnalyzer.joinSkeletons(tokens, 0, words));
  }

  private static JsonNode readResource() {
    try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
      return new ObjectMapper().readTree(in);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + RESOURCE + ": " + e.getMessage(), e);
    }
  }
}

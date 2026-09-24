package gr.uniwa.unihealth.backend.service.ai.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTextAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Screens a chat message for crisis language before the language model is ever called.
 *
 * <p>This is the first gate of the chat turn and the only one that can end it outright. The reason
 * it is deterministic rather than a classifier prompt is not latency, though it saves that too: a
 * student who types «θέλω να πεθάνω» must get fixed, reviewed text carrying 112 and 166, not an 8B
 * model's paraphrase of a crisis response. A paraphrase cannot be reviewed once, and every
 * regeneration is a fresh chance to get it wrong.
 *
 * <p><b>Why this is not tenant-scoped.</b> Unlike {@link
 * gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTermIndex}, whose rows live in
 * per-tenant databases and are editable, these phrases are a property of the safety layer rather
 * than of a university's content. They load once from the classpath into an immutable index, which
 * keeps a database round trip off the hot path and leaves the whole class testable with no Spring
 * context and no tenant bound.
 *
 * <p><b>Negation suppresses; attribution deliberately does not.</b> "I am not suicidal" is an
 * explicit denial and is suppressed, using the same clause-scoped rule as {@code MedicalTermIndex}.
 * But «ο φίλος μου θέλει να αυτοκτονήσει» still fires, and so does "my father has chest pain" -
 * because the right response to both is the same helpline and the same 112. Suppressing on
 * attribution would be borrowing a rule from label extraction, where mislabelling a student as
 * diabetic is the harm being prevented. Here the harms are not symmetrical: showing a card of
 * crisis numbers to someone who did not need it costs a card, while failing to show it to someone
 * who did is the failure this class exists to prevent.
 *
 * @author omaro
 */
@Slf4j
@Component
public class CrisisGuard {

  private static final String RESOURCE = "data/ai/crisis-terms.json";

  /**
   * Explicit denials, matched against the clause a term sits in.
   *
   * <p>Deliberately narrower than {@code MedicalTermIndex.NEGATION_CUES}. Words such as «ποτέ» and
   * "never" deny a fact, but "I never want to wake up" denies nothing - and treating it as a denial
   * would suppress exactly the message that most needs to fire.
   */
  private static final Set<String> DENIAL_CUES = Set.of(
      "δεν", "δε", "οχι", "χωρισ", "μη", "μην",
      "no", "not", "without", "denies", "denied");

  /** One authored phrase, flattened to the keys the matcher probes. */
  private record Term(CrisisTier tier, String stemKey, String surfaceKey, String skeletonKey,
                      String surface) {}

  private final Map<String, Term> byStem = new HashMap<>();
  private final Map<String, Term> bySurface = new HashMap<>();
  private final Map<String, Term> bySkeleton = new HashMap<>();
  private final Map<CrisisTier, String> messageKeys = new EnumMap<>(CrisisTier.class);
  private int maxTermWords = 1;

  /** Loads the committed dataset. This is the constructor Spring uses. */
  public CrisisGuard() {
    this(readResource());
  }

  /**
   * Builds the index from an already-parsed dataset, so the matching rules can be exercised from a
   * unit test with a handful of terms and no classpath resource.
   */
  public CrisisGuard(JsonNode dataset) {
    index(dataset);
  }

  /**
   * @param message what the student typed, in any of Greek, Greeklish, English, or a mix.
   * @return the tier the message falls into, or empty when it is safe to answer normally.
   */
  public Optional<CrisisAssessment> screen(String message) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(message);
    if (tokens.isEmpty()) {
      return Optional.empty();
    }

    // The earliest denial in each clause. Everything after it in that clause is suppressed, while a
    // term starting at or before the cue survives - which is what lets a phrase carrying its own
    // negation, «δεν θέλω να ζω», match as a single span and still fire.
    Map<Integer, Integer> denials = new HashMap<>();
    for (int i = 0; i < tokens.size(); i++) {
      if (DENIAL_CUES.contains(tokens.get(i).surface())) {
        denials.putIfAbsent(tokens.get(i).clause(), i);
      }
    }

    List<Term> hits = new ArrayList<>();

    // Longest span first. «δεν θέλω να ζω» has to beat «ζω» and "want to die" has to beat "die", or
    // a phrase would be judged by its least specific word.
    int i = 0;
    while (i < tokens.size()) {
      int consumed = 0;
      int maxSpan = Math.min(maxTermWords, tokens.size() - i);

      for (int n = maxSpan; n >= 1 && consumed == 0; n--) {
        Term hit = probe(tokens, i, n);
        if (hit != null) {
          // The span is consumed whether or not the hit survives the denial check: "not suicidal"
          // has still accounted for the word, and re-reading it as a shorter span would only find
          // the same term again.
          consumed = n;
          Integer cue = denials.get(tokens.get(i).clause());
          if (cue == null || i <= cue) {
            hits.add(hit);
          }
        }
      }

      i += consumed > 0 ? consumed : 1;
    }

    // Enum declaration order is severity order, so the most severe tier present wins.
    return hits.stream()
        .min(Comparator.comparing(Term::tier))
        .map(hit -> new CrisisAssessment(hit.tier(), messageKeys.get(hit.tier()), hit.surface()));
  }

  private Term probe(List<MedicalTextAnalyzer.Token> tokens, int from, int count) {
    Term hit = bySurface.get(MedicalTextAnalyzer.joinSurfaces(tokens, from, count));
    if (hit != null) {
      return hit;
    }

    hit = byStem.get(MedicalTextAnalyzer.joinStems(tokens, from, count));
    if (hit != null) {
      return hit;
    }

    // Latin script only: a student writing Greek in Latin letters ("thelo na pethano"). Tried last,
    // so a real English phrase is never mistaken for transliterated Greek.
    if (!tokens.get(from).greek()) {
      return bySkeleton.get(MedicalTextAnalyzer.joinSkeletons(tokens, from, count));
    }
    return null;
  }

  private void index(JsonNode dataset) {
    for (JsonNode tierNode : dataset.path("tiers")) {
      CrisisTier tier = CrisisTier.valueOf(tierNode.path("tier").asText());
      messageKeys.put(tier, tierNode.path("messageKey").asText());

      for (JsonNode termNode : tierNode.path("terms")) {
        register(tier, termNode.path("text").asText());
      }
    }

    if (bySurface.isEmpty()) {
      throw ExceptionUtils.createException(IllegalStateException.class, null,
          "Crisis term dictionary is empty. Refusing to start: an assistant that cannot "
              + "recognise a crisis message would answer it with generated text.");
    }

    log.info("Indexed {} crisis phrases across {} tiers, longest {} words.",
        bySurface.size(), messageKeys.size(), maxTermWords);
  }

  /**
   * Terms go through the very same tokenizer as user input, so an authored "can't breathe" and a
   * typed "can't breathe" both become {@code [can, t, breathe]} and meet. Authoring the
   * already-normalised form instead would silently fail to match.
   */
  private void register(CrisisTier tier, String text) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(text);
    if (tokens.isEmpty()) {
      log.warn("Crisis term [{}] normalised to nothing and was skipped.", text);
      return;
    }

    int words = tokens.size();
    maxTermWords = Math.max(maxTermWords, words);

    Term term = new Term(tier,
        MedicalTextAnalyzer.joinStems(tokens, 0, words),
        MedicalTextAnalyzer.joinSurfaces(tokens, 0, words),
        MedicalTextAnalyzer.joinSkeletons(tokens, 0, words),
        text);

    // First writer wins, and SELF_HARM is authored first, so a phrase appearing under both tiers
    // keeps the more severe one.
    bySurface.putIfAbsent(term.surfaceKey(), term);
    byStem.putIfAbsent(term.stemKey(), term);
    bySkeleton.putIfAbsent(term.skeletonKey(), term);
  }

  private static JsonNode readResource() {
    try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
      return new ObjectMapper().readTree(in);
    } catch (IOException e) {
      // Thrown rather than logged, unlike the NHS snapshot: a missing corpus costs citations, but a
      // missing crisis dictionary means self-harm messages reach the model. Also thrown directly
      // rather than through ExceptionUtils, which has no slot for a cause, and the stack trace is
      // the only useful thing about a boot failure.
      throw new IllegalStateException(
          "Could not read " + RESOURCE + ". Refusing to start without the crisis dictionary: "
              + e.getMessage(), e);
    }
  }
}

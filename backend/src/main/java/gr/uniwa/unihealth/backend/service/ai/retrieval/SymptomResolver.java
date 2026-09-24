package gr.uniwa.unihealth.backend.service.ai.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTextAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Works out which symptom page a question is about, before anything is embedded.
 *
 * <p><b>Why this exists when the embeddings are already cross-lingual.</b> {@code bge-m3} does
 * retrieve Greek-to-English on its own, so this is not what makes Greek work. What a resolved slug
 * buys is precision: it turns the dense search into a filtered search over one symptom's sections,
 * so «πονάει ο λαιμός μου» is answered from the sore-throat page rather than from the four pages
 * whose chunks happened to sit nearest in vector space. It also runs before the model, which means
 * the grounded path does not depend on {@code llama3.1:8b} choosing to call a tool - it frequently
 * does not.
 *
 * <p>The entity is used as a <em>filter</em>, not as a second ranked list. That is why there is no
 * rank fusion here: fusing a one-item lexical result with a dense list would be strictly worse than
 * simply restricting the dense list, and Reciprocal Rank Fusion's constants are tuned for web-scale
 * result sets rather than for 565 chunks.
 *
 * <p>Matching reuses {@link MedicalTextAnalyzer} and the two-stage technique already proven in
 * {@code MedicalTermIndex}: exact stem and skeleton keys first, longest span first. Unlike that
 * class this one does no fuzzy fallback - a wrong slug silently narrows retrieval to the wrong
 * page, which is worse than not narrowing at all, so an uncertain match is simply declined.
 *
 * @author omaro
 */
@Slf4j
@Component
public class SymptomResolver {

  private static final String RESOURCE = "data/ai/symptom-aliases.json";

  /**
   * Slug by lookup key. Seeded from the dataset at startup, then added to during ingest.
   *
   * <p>Concurrent maps because {@link #registerEnglish} is called from
   * {@code TenantContext.runForEachTenant}, which fans the tenants out across virtual threads.
   * Two tenants registering the same corpus into a plain {@code HashMap} would be writing to it at
   * once, which corrupts it silently rather than throwing - and the symptom would be a resolver
   * that intermittently stops matching, which is the hardest kind of bug to see here because a
   * declined match simply degrades retrieval instead of failing.
   */
  private final Map<String, String> byStem = new ConcurrentHashMap<>();
  private final Map<String, String> bySurface = new ConcurrentHashMap<>();
  private final Map<String, String> bySkeleton = new ConcurrentHashMap<>();

  /**
   * Keys with every word-final sigma removed, applied identically when registering and when
   * probing.
   *
   * <p>This exists because the stemmer cannot help here. {@code MedicalTextAnalyzer} refuses to
   * stem a word under five characters - a rule that protects short Greek stems from becoming traps
   * - so «πόνος» reduces to «πον» while the accusative «πόνο» stays whole, and the two never meet.
   * Yet «έχω πόνο στο στήθος» is how the phrase is actually typed.
   *
   * <p>Dropping a word-final sigma is not a hack but the Greek rule itself: masculine -ος/-ας/-ης
   * become -ο/-α/-η in the accusative, which is exactly the loss of one letter. Applying the same
   * lossy transform to both sides is the trick the skeleton key already uses for Greeklish, for the
   * same reason - a symmetric loss costs nothing, while an asymmetric one silently matches nothing.
   */
  private final Map<String, String> byLooseSurface = new ConcurrentHashMap<>();

  /**
   * Slug to the name a student would recognise, for the rare places something is shown rather than
   * matched - a conversation title in the history rail, today.
   *
   * <p>The first alias in the file, deliberately: that file authors lay words before clinical ones,
   * because students type the former, so «πονοκέφαλος» is the entry and «κεφαλαλγία» is not. It is
   * also Greek, which the slug and the NHS page title are not, and the interface is Greek-first.
   *
   * <p>Recorded straight from the dataset rather than inside {@link #register}, because a term too
   * short to be a safe lookup key is still a perfectly good name.
   */
  private final Map<String, String> nameBySlug = new ConcurrentHashMap<>();

  /**
   * Longest registered term, in words, which bounds the span the probe tries.
   *
   * <p>Volatile for the same reason the maps are concurrent: ingest writes it from a virtual
   * thread per tenant while requests read it. The update is not atomic, but every tenant
   * registers the same corpus and so computes the same maximum, and a lost write could only ever
   * leave it too small for an instant - which costs one span on one lookup, not correctness.
   */
  private volatile int maxTermWords = 1;

  public SymptomResolver() {
    this(readResource());
  }

  /** Test seam: build from an inline dataset instead of the committed file. */
  public SymptomResolver(JsonNode dataset) {
    index(dataset);
  }

  /**
   * Adds the English titles and synonyms held in the database, so the resolver covers both
   * languages without the alias file having to repeat what the corpus already says.
   *
   * <p>Called once per tenant during ingest. Registration is idempotent and first-writer-wins, so a
   * second tenant whose corpus is identical changes nothing.
   */
  public void registerEnglish(String slug, String title, String synonyms) {
    register(slug, title);

    // putIfAbsent, so Greek always wins the display name. This only supplies one for a symptom the
    // alias file does not cover at all, where an English title beats showing the raw slug.
    if (StringUtils.isNotBlank(title)) {
      nameBySlug.putIfAbsent(slug, title);
    }

    // Synonyms are a comma-separated list on the symptom row, and each is its own way in:
    // "heart pain" has to reach chest-pain.
    if (StringUtils.isNotBlank(synonyms)) {
      for (String synonym : synonyms.split(",")) {
        register(slug, synonym);
      }
    }
  }

  /**
   * @return the slug the question is about, or empty when nothing matched confidently.
   */
  public Optional<String> resolve(String question) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(question);
    if (tokens.isEmpty()) {
      return Optional.empty();
    }

    for (int i = 0; i < tokens.size(); i++) {
      int maxSpan = Math.min(maxTermWords, tokens.size() - i);

      // Longest span first, so «πόνος στο στήθος» beats a bare «πόνος» that matches nothing useful.
      for (int n = maxSpan; n >= 1; n--) {
        String slug = probe(tokens, i, n);
        if (slug != null) {
          // First match wins rather than best-of-all. A question naming two symptoms is better
          // served by narrowing to the first than by an arbitrary choice between them, and the
          // unfiltered dense search still covers the second.
          return Optional.of(slug);
        }
      }
    }

    return Optional.empty();
  }

  /**
   * @return the name a student would recognise for a slug, or empty for one never registered.
   */
  public Optional<String> nameFor(String slug) {
    return Optional.ofNullable(nameBySlug.get(slug));
  }

  /**
   * What a question is about, in words rather than in a slug.
   *
   * <p>Exists so a conversation can be titled by its subject without a second generation. The
   * matching has already been paid for - the same call narrows retrieval - and this adds one map
   * lookup on top of it, which is the entire reason the history rail does not ask
   * {@code llama3.1:8b} to summarise anything.
   *
   * <p>Empty whenever {@link #resolve} declines, which is often and by design: a question about
   * sleep or exam stress names no symptom, and inventing a topic for it would be worse than
   * falling back to what the student actually typed.
   */
  public Optional<String> topicOf(String question) {
    return resolve(question).flatMap(this::nameFor);
  }

  private String probe(List<MedicalTextAnalyzer.Token> tokens, int from, int count) {
    String slug = bySurface.get(MedicalTextAnalyzer.joinSurfaces(tokens, from, count));
    if (slug != null) {
      return slug;
    }

    slug = byStem.get(MedicalTextAnalyzer.joinStems(tokens, from, count));
    if (slug != null) {
      return slug;
    }

    slug = byLooseSurface.get(
        dropFinalSigma(MedicalTextAnalyzer.joinSurfaces(tokens, from, count)));
    if (slug != null) {
      return slug;
    }

    if (!tokens.get(from).greek()) {
      // Greeklish is folded the same way: the Greek sigma has already become a Latin s by the time
      // the skeleton is built, so "ponokefalo" and «πονοκέφαλος» meet once both lose it.
      return bySkeleton.get(dropFinalSigma(MedicalTextAnalyzer.joinSkeletons(tokens, from, count)));
    }
    return null;
  }

  /**
   * Removes a word-final sigma - Greek «σ» or its transliterated «s» - from every word long enough
   * that the letter is an ending rather than part of the root.
   */
  private static String dropFinalSigma(String key) {
    StringBuilder loose = new StringBuilder(key.length());

    for (String word : key.split(" ")) {
      boolean strippable = word.length() >= MedicalTextAnalyzer.MIN_KEY_LENGTH + 1
          && (word.endsWith("σ") || word.endsWith("s"));

      loose.append(strippable ? word.substring(0, word.length() - 1) : word).append(' ');
    }

    return loose.toString().trim();
  }

  private void index(JsonNode dataset) {
    JsonNode aliases = dataset.path("aliases");

    aliases.fieldNames().forEachRemaining(slug -> {
      JsonNode terms = aliases.path(slug);

      // path(0) rather than get(0): a slug whose array is empty, or somehow not an array at all,
      // should cost that symptom its display name and nothing else.
      String name = terms.path(0).asText();

      if (StringUtils.isNotBlank(name)) {
        nameBySlug.putIfAbsent(slug, name);
      }

      terms.forEach(term -> register(slug, term.asText()));
    });

    log.info("Symptom resolver indexed {} lookup keys across {} symptoms, longest {} words.",
        bySurface.size(), aliases.size(), maxTermWords);
  }

  private void register(String slug, String text) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(text);
    if (tokens.isEmpty()) {
      return;
    }

    int words = tokens.size();

    // A one-word key shorter than the analyzer's floor is not registered at all. This is the same
    // rule MedicalTermIndex uses and for the same reason: short stems are traps. «πόνος» stems to
    // «πον», which would make every sentence mentioning any pain resolve to whichever page
    // happened to be registered first.
    if (words == 1 && tokens.getFirst().surface().length() < MedicalTextAnalyzer.MIN_KEY_LENGTH) {
      return;
    }

    maxTermWords = Math.max(maxTermWords, words);

    String surface = MedicalTextAnalyzer.joinSurfaces(tokens, 0, words);

    bySurface.putIfAbsent(surface, slug);
    byStem.putIfAbsent(MedicalTextAnalyzer.joinStems(tokens, 0, words), slug);
    byLooseSurface.putIfAbsent(dropFinalSigma(surface), slug);
    bySkeleton.putIfAbsent(
        dropFinalSigma(MedicalTextAnalyzer.joinSkeletons(tokens, 0, words)), slug);
  }

  private static JsonNode readResource() {
    try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
      return new ObjectMapper().readTree(in);
    } catch (IOException e) {
      // Not fatal, unlike the crisis dictionary: without aliases the assistant still answers, just
      // with unfiltered retrieval. Degrading is the proportionate response to losing a precision
      // layer.
      throw new IllegalStateException("Could not read " + RESOURCE + ": " + e.getMessage(), e);
    }
  }
}

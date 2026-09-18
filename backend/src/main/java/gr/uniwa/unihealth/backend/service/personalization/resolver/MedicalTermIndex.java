package gr.uniwa.unihealth.backend.service.personalization.resolver;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import gr.uniwa.unihealth.backend.repository.LabelKeywordMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The medical term dictionary, held in memory and matched against free text.
 *
 * <p>Replaces a scan that compared every dictionary keyword against every n-gram of the input on
 * every profile save — up to roughly 71,000 edit-distance computations, with the <em>slowest</em>
 * path being the one where nothing matched. Matching is now a handful of hash probes per token,
 * with edit distance reserved for the words nothing explained, so cost is bounded by the length of
 * what the student wrote and is independent of how large the dictionary grows.
 *
 * <p>Held per tenant because the rows live in per-tenant databases. Each index is immutable once
 * built and swapped in wholesale, so the read path needs no locking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalTermIndex {

  private final LabelKeywordMappingRepository repository;
  private final TenantContext tenantContext;

  private final Map<String, TenantIndex> byTenant = new ConcurrentHashMap<>();

  /**
   * Similarity thresholds.
   *
   * <p>{@link #JARO_WINKLER_FLOOR} vetoes candidates whose opening characters disagree. Jaro-Winkler
   * weights a shared prefix, which encodes something true of medical vocabulary: the front of the
   * word is the diagnostic part. Plain edit distance treats a wrong letter in position one and
   * position nine alike, which is how {@code celery} and {@code celiac} end up neighbours.
   *
   * <p>{@link #TRANSPOSITION_RESCUE} readmits a candidate one edit over budget when the two strings
   * are otherwise near-identical. Swapping adjacent letters is the commonest typing slip and plain
   * Levenshtein charges it two, so {@code peantu} would miss {@code peanut}. Damerau-Levenshtein
   * would handle it directly but is not in Commons Text; Jaro-Winkler scores transpositions
   * generously, which gets the same result from a class already on the classpath.
   */
  private static final double JARO_WINKLER_FLOOR = 0.88;
  private static final double TRANSPOSITION_RESCUE = 0.95;

  private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

  /**
   * Threshold-bounded distance calculators, indexed by their own threshold.
   *
   * <p>Commons Text fills only a diagonal band and bails out as soon as the budget is exceeded,
   * where the hand-rolled version this replaces always filled the whole row. The instances are
   * stateless and thread-safe, so they are shared.
   */
  private static final LevenshteinDistance[] DISTANCE = {
      LevenshteinDistance.getDefaultInstance(),
      new LevenshteinDistance(1), new LevenshteinDistance(2),
      new LevenshteinDistance(3), new LevenshteinDistance(4)
  };

  /** Greek and English cues that a condition is being denied rather than reported. */
  private static final Set<String> NEGATION_CUES = Set.of(
      "δεν", "δε", "οχι", "ουτε", "χωρισ", "μη", "μην", "ποτε", "καμια", "κανενα", "κανεναν",
      "no", "not", "none", "without", "never", "denies", "denied", "negative");

  /**
   * Cues that a condition belongs to somebody else. Not negation, but it has to suppress just as
   * hard — this is what stops "οικογενειακό ιστορικό διαβήτη" reporting that the student is
   * diabetic because a relative is.
   */
  private static final Set<String> ATTRIBUTION_CUES = Set.of(
      "οικογενειακο", "κληρονομικο", "πατερασ", "μητερα", "αδερφοσ", "αδερφη", "γιαγια", "παππουσ",
      "family", "father", "mother", "brother", "sister", "grandmother", "grandfather");

  /** One dictionary term, flattened for matching. */
  public record Entry(String labelCode, String conceptId, String termType, String scope,
                      String surface, int words, int priority, boolean exactOnly, boolean weak) {}

  private record TenantIndex(Map<String, Entry> exact, List<Entry> fuzzy, int maxTermWords,
                             Map<String, Integer> priorities,
                             Map<String, Integer> lowestPriorityByScope) {}

  /**
   * What one free-text field yielded.
   *
   * @param negationSeen whether a negation or attribution cue suppressed something. The caller needs
   *                     this: "δεν έχω αλλεργίες" produces no labels, but it must not then be
   *                     treated as "we could not understand this" and answered with
   *                     {@code ALLERGY_OTHER}.
   */
  public record MatchResult(Set<String> labels, boolean negationSeen) {}

  /** Rebuilds the current tenant's index from the database. Called after seeding. */
  public void rebuild() {
    String tenant = tenantContext.getCurrentTenant();
    TenantIndex index = build(repository.findByActiveTrue());
    byTenant.put(tenant, index);
    log.info("Medical term index for tenant [{}]: {} keys, {} fuzzy-eligible, longest term {} words",
        tenant, index.exact().size(), index.fuzzy().size(), index.maxTermWords());
  }

  private TenantIndex current() {
    return byTenant.computeIfAbsent(tenantContext.getCurrentTenant(),
        tenant -> build(repository.findByActiveTrue()));
  }

  private TenantIndex build(List<LabelKeywordMapping> rows) {
    Map<String, Entry> exact = new HashMap<>();
    List<Entry> fuzzy = new ArrayList<>();
    Map<String, Integer> priorities = new HashMap<>();
    Map<String, Integer> lowestByScope = new HashMap<>();
    int maxTermWords = 1;

    for (LabelKeywordMapping row : rows) {
      List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(row.getKeyword());
      if (tokens.isEmpty()) {
        continue;
      }

      int words = tokens.size();
      maxTermWords = Math.max(maxTermWords, words);

      boolean exactOnly = isExactOnly(row.getTermType());
      String surface = MedicalTextAnalyzer.joinSurfaces(tokens, 0, words);

      Entry entry = new Entry(row.getLabelCode(), row.getConceptId(), row.getTermType(),
          row.getKeywordType(), surface, words,
          row.getPriority() != null ? row.getPriority() : 0,
          exactOnly, "WEAK".equals(row.getTermType()));

      if (row.getPriority() != null) {
        priorities.put(row.getLabelCode(), row.getPriority());
        lowestByScope.merge(row.getKeywordType(), row.getPriority(), Math::min);
      }

      String namespace = row.getKeywordType() + "|" + row.getLang();
      String stemKey = MedicalTextAnalyzer.joinStems(tokens, 0, words);

      // A stem below the floor is not registered, and the surface form carries the term instead.
      // τόνος stems to "τον", which is also the Greek definite article; registering it would make
      // every Greek sentence containing "τον" report a fish allergy.
      if (stemKey.length() >= MedicalTextAnalyzer.MIN_KEY_LENGTH) {
        register(exact, namespace + ":" + stemKey, entry);
      }
      register(exact, namespace + "|s:" + surface, entry);

      if ("el".equals(row.getLang())) {
        String skeleton = MedicalTextAnalyzer.joinSkeletons(tokens, 0, words);
        if (skeleton.length() >= MedicalTextAnalyzer.MIN_KEY_LENGTH) {
          register(exact, row.getKeywordType() + "|gl:" + skeleton, entry);
        }
      }

      if (words == 1 && !exactOnly) {
        fuzzy.add(entry);
      }
    }

    return new TenantIndex(Map.copyOf(exact), List.copyOf(fuzzy), maxTermWords,
        Map.copyOf(priorities), Map.copyOf(lowestByScope));
  }

  /**
   * Adds a key, refusing to let two different concepts share one.
   *
   * <p>This is the check that makes an aggressive stemmer safe to run: over-stemming is harmless
   * right up until two unrelated ideas collapse onto the same key, and that is a property you can
   * test rather than a judgement you have to trust. A bad dictionary edit must not stop the
   * application starting, so the later term is dropped — but it is logged loudly enough to find.
   */
  private void register(Map<String, Entry> exact, String key, Entry entry) {
    Entry existing = exact.get(key);
    if (existing != null && !existing.labelCode().equals(entry.labelCode())) {
      log.error("Term key [{}] is claimed by both {} and {}; ignoring the second. One of the two "
              + "concepts needs a more specific term in medical-terms.json",
          key, existing.conceptId(), entry.conceptId());
      return;
    }
    exact.putIfAbsent(key, entry);
  }

  private static boolean isExactOnly(String termType) {
    return "ABBREVIATION".equals(termType) || "MISSPELLING".equals(termType)
        || "WEAK".equals(termType);
  }

  /** The dataset's priority for a label code, or {@code null} if it is not a dictionary concept. */
  public Integer priorityOf(String labelCode) {
    return current().priorities().get(labelCode);
  }

  /**
   * The lowest priority any concept in a field carries, or {@code null} if the dictionary has none.
   *
   * <p>Exists so the "we did not recognise this" fallback can be placed beneath every condition the
   * dictionary <em>does</em> know, and stay there. Deriving it rather than hardcoding a number means
   * adding a low-priority concept later cannot silently push the fallback above it.
   */
  public Integer lowestPriorityIn(String scope) {
    return current().lowestPriorityByScope().get(scope);
  }

  /**
   * Matches one free-text field.
   *
   * @param scope ALLERGY or CHRONIC — terms are only considered against the field they belong to.
   */
  public MatchResult match(String text, String scope) {
    List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize(text);
    if (tokens.isEmpty()) {
      return new MatchResult(Set.of(), false);
    }

    TenantIndex index = current();

    // The earliest cue in each clause. Everything after it in that clause is suppressed; anything
    // before it survives, so "peanuts, but no dairy" keeps the peanut allergy.
    Map<Integer, Integer> cues = new HashMap<>();
    for (int i = 0; i < tokens.size(); i++) {
      String word = tokens.get(i).surface();
      if (NEGATION_CUES.contains(word) || ATTRIBUTION_CUES.contains(word)) {
        cues.putIfAbsent(tokens.get(i).clause(), i);
      }
    }

    boolean[] claimed = new boolean[tokens.size()];
    List<Entry> hits = new ArrayList<>();
    boolean[] suppressed = {false};

    // Longest span first. Maximal munch is the right rule for a dictionary where the short terms
    // are broader than the long ones: "ulcerative colitis" must beat "colitis", "υψηλή πίεση" must
    // beat anything shorter, and "φιστίκι Αιγίνης" (pistachio) must beat "φιστίκι".
    int i = 0;
    while (i < tokens.size()) {
      int consumed = 0;
      int maxSpan = Math.min(index.maxTermWords(), tokens.size() - i);

      for (int n = maxSpan; n >= 1 && consumed == 0; n--) {
        Entry hit = probe(index, tokens, i, n, scope);
        if (hit != null) {
          // The span is consumed whether or not the hit survives negation: "no peanuts" has still
          // accounted for the word "peanuts", and re-examining it as a shorter span would only
          // find the same term again.
          accept(hit, tokens, i, n, cues, claimed, hits, suppressed);
          consumed = n;
        }
      }

      i += consumed > 0 ? consumed : 1;
    }

    // Only words the exact sweep could not explain, and only one word at a time: a multi-word term
    // is never approximated as a whole, its parts are.
    for (int j = 0; j < tokens.size(); j++) {
      if (claimed[j]) {
        continue;
      }

      // Take the *closest* term rather than the first acceptable one. Scanning in row order and
      // stopping at the first hit makes the answer depend on how the dictionary happens to be
      // ordered, which is not something the dataset should be able to influence.
      Entry best = null;
      double bestScore = 0;
      for (Entry entry : index.fuzzy()) {
        if (!entry.scope().equals(scope)) {
          continue;
        }
        double score = fuzzyScore(tokens.get(j).surface(), entry);
        if (score > bestScore) {
          bestScore = score;
          best = entry;
        }
      }

      if (best != null) {
        accept(best, tokens, j, 1, cues, claimed, hits, suppressed);
      }
    }

    // WEAK terms are a last resort: bare "thyroid" or "colitis" only mean anything when nothing
    // more specific was said in the same field.
    List<Entry> strong = hits.stream().filter(hit -> !hit.weak()).toList();
    List<Entry> kept = strong.isEmpty() ? hits : strong;

    Set<String> labels = new LinkedHashSet<>();
    kept.forEach(hit -> labels.add(hit.labelCode()));
    return new MatchResult(Set.copyOf(labels), suppressed[0]);
  }

  private Entry probe(TenantIndex index, List<MedicalTextAnalyzer.Token> tokens, int from, int count,
      String scope) {
    boolean greek = tokens.get(from).greek();
    String namespace = scope + "|" + (greek ? "el" : "en");

    Entry hit = index.exact().get(namespace + ":" + MedicalTextAnalyzer.joinStems(tokens, from, count));
    if (hit != null) {
      return hit;
    }

    hit = index.exact()
        .get(namespace + "|s:" + MedicalTextAnalyzer.joinSurfaces(tokens, from, count));
    if (hit != null) {
      return hit;
    }

    // Latin script only: a student writing Greek in Latin letters ("diavitis"). Tried after the
    // English namespace, so a real English word is never mistaken for transliterated Greek.
    if (!greek) {
      return index.exact()
          .get(scope + "|gl:" + MedicalTextAnalyzer.joinSkeletons(tokens, from, count));
    }
    return null;
  }

  private boolean accept(Entry entry, List<MedicalTextAnalyzer.Token> tokens, int from, int count,
      Map<Integer, Integer> cues, boolean[] claimed, List<Entry> hits, boolean[] suppressed) {
    for (int i = from; i < from + count; i++) {
      claimed[i] = true;
    }

    Integer cue = cues.get(tokens.get(from).clause());
    if (cue != null && from > cue) {
      suppressed[0] = true;
      return false;
    }

    hits.add(entry);
    return true;
  }

  /**
   * The edit budget, keyed on the <em>dictionary</em> term's length rather than the input's.
   *
   * <p>The previous implementation used a flat budget of two with no minimum length at all, which
   * made every short entry a false-positive generator: {@code ibs} matched {@code its},
   * {@code egg} matched {@code leg}, {@code rye} matched {@code eye} and {@code t2d} matched
   * {@code t20}. Anything of four characters or fewer now has to match exactly.
   */
  private static int maxEdits(int termLength) {
    if (termLength <= 4) {
      return 0;
    }
    if (termLength <= 7) {
      return 1;
    }
    if (termLength <= 11) {
      return 2;
    }
    return 3;
  }

  /**
   * How well a span matches a term, or {@code 0} if it is not an acceptable match at all.
   *
   * <p>Cheapest tests first, so most candidates are rejected before any distance is computed. The
   * returned score is the Jaro-Winkler similarity, which is only meaningful for ranking the
   * candidates that already passed the edit-distance budget.
   */
  private double fuzzyScore(String span, Entry term) {
    if (term.exactOnly()) {
      return 0;
    }

    int budget = maxEdits(term.surface().length());
    if (budget == 0 || Math.abs(span.length() - term.surface().length()) > budget) {
      return 0;
    }
    if (budget <= 2 && span.charAt(0) != term.surface().charAt(0)) {
      return 0;
    }

    Double similarity = JARO_WINKLER.apply(span, term.surface());
    if (similarity == null || similarity < JARO_WINKLER_FLOOR) {
      return 0;
    }

    int distance = DISTANCE[Math.min(budget + 1, DISTANCE.length - 1)]
        .apply(span, term.surface());
    if (distance < 0) {
      return 0;
    }

    boolean accepted = distance <= budget || similarity >= TRANSPOSITION_RESCUE;
    return accepted ? similarity : 0;
  }
}

package gr.uniwa.unihealth.backend.service.personalization.resolver;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import gr.uniwa.unihealth.backend.repository.LabelKeywordMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * HealthDetailLabelExtractor — Free-Text Medical Keyword Extractor
 * <p>
 * ───────────────────────────────────────────────────────────────── ALGORITHMS AND TECHNIQUES USED
 * IN THIS CLASS: ─────────────────────────────────────────────────────────────────
 * <p>
 * 1. LEVENSHTEIN EDIT DISTANCE (Vladimir Levenshtein, 1965) → Used in: levenshtein(),
 * isLikelyMatch() → Category: Dynamic Programming / String Metrics → Purpose: Typo-tolerant
 * matching — measures how many single-character edits (insert, delete, substitute) separate two
 * strings. → Variant used: Space-optimised single-row DP (O(m) space vs O(n×m) full matrix) → Used
 * in broader algorithms: Fuzzy search engines (Elasticsearch), spell checkers, DNA sequence
 * alignment (bioinformatics), plagiarism detection, OCR post-processing.
 * <p>
 * 2. N-GRAM TOKENISATION → Used in: buildTokens() → Category: Natural Language Processing (NLP) /
 * Information Retrieval → Purpose: Breaks text into overlapping word sequences (unigrams, bigrams,
 * trigrams) so multi-word medical phrases like "high blood pressure" or "irritable bowel syndrome"
 * can be matched as single units. → Used in broader algorithms: Language models (n-gram LMs),
 * TF-IDF, BM25 ranking, Google's word2vec predecessor, Named Entity Recognition (NER).
 * <p>
 * 3. UNICODE NFD NORMALISATION + DIACRITIC STRIPPING → Used in: normalizeText() → Category: Unicode
 * Text Processing (Unicode Standard, Unicode Consortium) → Purpose: Decomposes composed characters
 * (é → e + ́) and removes combining marks so accented user input still matches ASCII keywords. →
 * Used in broader algorithms: Any internationalized search or NLP pipeline, information retrieval
 * systems, cross-language text matching.
 * <p>
 * 4. LIGHT RULE-BASED STEMMING (inspired by Porter Stemmer, Martin Porter 1980) → Used in:
 * normalizeWord(), normalizeCommonVariants() → Category: Morphological Analysis / NLP Preprocessing
 * → Purpose: Reduces inflected word forms to approximate root (allergies→allergy, peanuts→peanut)
 * so plural/singular variants match the same keyword without duplicating the dictionary. → Current
 * implementation: Handles only common English plural suffixes (-ies, -es, -s). NOT a full stemmer.
 * → Used in broader algorithms: Porter Stemmer, Snowball Stemmer, Lovins Stemmer,
 * Lucene/Elasticsearch analysis chain.
 * <p>
 * 5. WHOLE-PHRASE BOUNDARY MATCHING → Used in: containsWholePhrase() → Category: String Pattern
 * Matching → Purpose: Prevents partial substring false positives by padding text with spaces before
 * checking containment — "cod" won't match inside "coconut" using this technique. → Related to:
 * Knuth-Morris-Pratt (KMP), Boyer-Moore substring search, word-boundary regex (\b) matching.
 * <p>
 * ───────────────────────────────────────────────────────────────── ALGORITHMS SUGGESTED FOR FUTURE
 * IMPROVEMENT (see TODO comments):
 * ─────────────────────────────────────────────────────────────────
 * <p>
 * A. PORTER STEMMER / SNOWBALL STEMMER → Would replace the current light stemmer in normalizeWord()
 * → Handles far more suffix rules: -ing, -tion, -ment, -ness, -ical etc. → Library: Apache Lucene's
 * SnowballStemmer or Apache OpenNLP
 * <p>
 * B. BM25 (Best Match 25 — Robertson & Spärck Jones, 1994) → Would replace the current binary
 * match/no-match approach → Ranks keyword matches by term frequency and inverse document frequency
 * → Returns a relevance score instead of boolean — better for ranking which condition is most
 * prominently described in the free text
 * <p>
 * C. NAMED ENTITY RECOGNITION (NER) → Would replace the entire keyword dictionary approach → A
 * trained ML model (e.g. BioBERT, cTAKES, Apache OpenNLP MedModel) identifies medical entities
 * directly from free text → Much higher accuracy — handles completely novel phrasings → Library:
 * Apache OpenNLP, Stanford NER, Hugging Face BioBERT
 * <p>
 * D. JARO-WINKLER DISTANCE (Jaro 1989, Winkler 1990) → Alternative to Levenshtein for short strings
 * and proper nouns → Gives higher weight to prefix matches — better for medical term names where
 * the beginning of the word is most distinctive ("diabet" matches "diabetes" better than
 * Levenshtein would rank it) → Could replace or complement levenshtein() in isLikelyMatch()
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthDetailLabelExtractor {

  private final LabelKeywordMappingRepository keywordRepository;

  /**
   * Fuzzy-match threshold for Levenshtein edit distance.
   * <p>
   * Algorithm: Levenshtein Distance (Vladimir Levenshtein, 1965)
   * <p>
   * Value 2 tolerates: - one transposition ("peantu" → "peanut") - one missing character ("penut" →
   * "peanut") - one extra character ("peannutt" → "peanut")
   * <p>
   * Increasing this value improves recall (catches more typos) but reduces precision (more false
   * positives).
   * <p>
   * TODO: Consider making this threshold configurable per keyword length —
   *       short keywords like "ibs" (3 chars) should use threshold 0 or 1
   *       to avoid matching unrelated short tokens. Long keywords like
   *       "gastroesophageal reflux" can safely tolerate threshold 2.
   */
  private static final int FUZZY_THRESHOLD = 2;

  private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
  private static final Pattern NON_ALNUM_SPACE = Pattern.compile("[^a-z0-9\\s]");

  public List<String> extract(HealthProfileDTO dto) {
    Set<String> labels = new HashSet<>();

    List<LabelKeywordMapping> allergyKeywords =
        keywordRepository.findByKeywordTypeAndActiveTrue("ALLERGY");
    List<LabelKeywordMapping> chronicKeywords =
        keywordRepository.findByKeywordTypeAndActiveTrue("CHRONIC");

    boolean allergyMatched =
        extractFromText(dto.getFoodAllergiesDetails(), allergyKeywords, labels, "ALLERGY");
    boolean chronicMatched =
        extractFromText(dto.getChronicConditionsDetails(), chronicKeywords, labels, "CHRONIC");

    if (dto.isHasFoodAllergies() && hasText(dto.getFoodAllergiesDetails()) && !allergyMatched) {
      log.warn("No allergy mapping found for input: [{}]", dto.getFoodAllergiesDetails());
      labels.add("ALLERGY_OTHER");
    }

    if (dto.isHasChronicConditions() && hasText(
        dto.getChronicConditionsDetails()) && !chronicMatched) {
      log.warn("No chronic-condition mapping found for input: [{}]",
          dto.getChronicConditionsDetails());
      labels.add("CHRONIC_OTHER");
    }

    return new ArrayList<>(labels);
  }

  /**
   * Core matching pipeline — applies the full extraction algorithm to a single text field.
   * <p>
   * Pipeline steps (standard Information Retrieval pattern): 1. Normalise input text     → Unicode
   * NFD + light stemming 2. Tokenise into n-grams    → unigrams, bigrams, trigrams 3. Exact phrase
   * match       → whole-phrase boundary check 4. Fuzzy token match        → Levenshtein edit
   * distance
   * <p>
   * This two-stage approach (exact first, fuzzy second) is standard in search engine query
   * processing — exact matches are cheaper to compute and should always take priority over
   * approximate ones.
   * <p>
   * TODO: Consider adding a BM25 relevance score per matched label instead
   *       of a binary matched/not-matched boolean. BM25 (Robertson & Spärck Jones, 1994)
   *       would allow ranking which condition appears most prominently in
   *       the free text when multiple conditions are mentioned.
   *
   * TODO: Consider adding a confidence score per match (1.0 = exact, <1.0 = fuzzy)
   *       and storing it alongside the label so the content engine can weight
   *       fuzzy-matched labels lower than exact-matched ones.
   */
  private boolean extractFromText(String text, List<LabelKeywordMapping> keywords,
      Set<String> labels, String type) {

    if (!hasText(text)) {
      return false;
    }

    String normalized = normalizeText(text);
    List<String> tokens = buildTokens(normalized);

    boolean matched = false;
    Set<String> matchedKeywords = new HashSet<>();

    for (LabelKeywordMapping entry : keywords) {
      String keyword = normalizeText(entry.getKeyword());

      // Stage 1: Exact phrase match (O(n) string search)
      // Uses whole-phrase boundary padding — see containsWholePhrase()
      if (containsWholePhrase(normalized, keyword)) {
        labels.add(entry.getLabelCode());
        matched = true;
        matchedKeywords.add(keyword);
        continue;
      }

      // Stage 2: Token-level fuzzy match using Levenshtein distance
      // Iterates over all n-gram tokens and checks edit distance
      // TODO: Replace or complement with Jaro-Winkler distance for
      //       short medical keywords and proper nouns (disease names).
      //       Jaro-Winkler (Jaro 1989, Winkler 1990) gives higher weight
      //       to prefix agreement — better for terms like "diabet*" or "asthmat*"
      for (String token : tokens) {
        if (isLikelyMatch(token, keyword)) {
          labels.add(entry.getLabelCode());
          matched = true;
          matchedKeywords.add(keyword);
          break;
        }
      }
    }

    if (matched) {
      log.info("Matched {} input [{}] to labels {}", type, text, labels);
    }

    return matched;
  }

  private boolean hasText(String text) {
    return text != null && !text.isBlank();
  }

  /**
   * Normalises free-text input into a canonical searchable form.
   * <p>
   * Techniques used in this method:
   * <p>
   * 1. Unicode NFD Normalisation (Unicode Standard) Decomposes composed characters into base
   * character + combining marks. "é" → "e" + combining acute accent Used in all international NLP
   * pipelines and search engines.
   * <p>
   * 2. Diacritic Stripping (\p{M} removal) Removes all combining diacritical marks after NFD
   * decomposition. "café" → "cafe", "naïve" → "naive"
   * <p>
   * 3. Lowercasing (locale-aware) Uses Locale.ROOT to avoid locale-specific case folding bugs (e.g.
   * Turkish dotless-i issue).
   * <p>
   * 4. Punctuation normalisation Replaces all non-alphanumeric characters with spaces. "Crohn's" →
   * "Crohn s" → after custom fix → "Crohns"
   * <p>
   * 5. Light rule-based stemming See normalizeCommonVariants() — inspired by Porter Stemmer.
   * <p>
   * TODO: Consider replacing steps 4+5 with a full tokenisation pipeline
   *       using Apache OpenNLP or Lucene's StandardAnalyzer which handles
   *       punctuation, stop words, and stemming in a single integrated pass.
   * <p>
   * TODO: Add stop word removal (a, the, my, i, have, been, diagnosed with...)
   *       These common words currently pollute the token list and can cause
   *       false fuzzy matches. Standard stop word lists exist in Apache Lucene
   *       and NLTK (Python) — implementing one here would improve precision.
   */
  private String normalizeText(String input) {
    if (input == null) {
      return "";
    }

    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")   // remove accents
        .toLowerCase(Locale.ROOT);

    normalized = NON_ALNUM_SPACE.matcher(normalized).replaceAll(" ");
    normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();

    // normalize apostrophe variants after punctuation cleanup
    normalized = normalized.replace("crohn s", "crohns");

    // small morphology normalization
    normalized = normalizeCommonVariants(normalized);

    return normalized;
  }

  /**
   * Applies lightweight rule-based morphological normalisation word by word.
   * <p>
   * Technique: Simplified Stemming Related to: Porter Stemmer (Martin Porter, 1980), Snowball
   * Stemmer
   * <p>
   * The Porter Stemmer defines ~60 suffix-stripping rules across 5 phases. This implementation
   * covers only the most common English plural forms — intentionally limited to reduce
   * over-stemming risk in medical vocabulary.
   * <p>
   * Over-stemming example (why full Porter is risky here): Porter would stem "nursing" → "nurs" But
   * "nursing" is a meaningful medical context word that should be preserved.
   * <p>
   * TODO: Evaluate replacing this with the Snowball Stemmer for English
   *       (available via Apache Lucene: org.apache.lucene.analysis.en.EnglishAnalyzer)
   *       Test carefully against the medical keyword dictionary first —
   *       some medical terms stem incorrectly with general-purpose stemmers.
   * <p>
   * TODO: Consider a medical-domain lemmatiser instead of a stemmer.
   *       Lemmatisation (returns actual dictionary root: "diagnoses" → "diagnosis")
   *       is more accurate than stemming for clinical terminology.
   *       Libraries: Apache cTAKES, MetaMap (NLM), clinical BERT tokeniser.
   */
  private String normalizeCommonVariants(String text) {
    String[] words = text.split("\\s+");
    List<String> normalizedWords = new ArrayList<>();

    for (String word : words) {
      normalizedWords.add(normalizeWord(word));
    }

    return String.join(" ", normalizedWords);
  }

  /**
   * Strips common English plural suffixes from a single word.
   * <p>
   * Algorithm: Subset of Porter Stemmer Phase 1a rules (Martin Porter, 1980)
   * <p>
   * Rules applied (in priority order): -ies → -y    ("allergies" → "allergy") -es  → "" ("rashes" →
   * "rash") with exclusions for -sses/-shes/-ches -s   → ""    ("peanuts" → "peanut") excluding -ss
   * endings
   * <p>
   * Exclusions prevent incorrect stems: "sses" exclusion: "grasses" stays "grasses" (not "grass"
   * via this rule) "ss"   exclusion: "asthma" related terms with double-s are not wrongly stripped
   * <p>
   * TODO: Add handling for common medical derivational suffixes:
   *   -itis  (gastritis → gastrit — but this may be too aggressive)
   *   -osis  (fibrosis → fibros)
   *   -emia  (anemia stays — already in dictionary)
   *   These are risky — test against dictionary before adding.
   */
  private String normalizeWord(String word) {
    if (word == null || word.isBlank()) {
      return word;
    }

    // common English plural simplifications
    if (word.endsWith("ies") && word.length() > 3) {
      return word.substring(0, word.length() - 3) + "y";
    }

    if (word.endsWith("es") && word.length() > 3 && !word.endsWith("sses") && !word.endsWith(
        "shes") && !word.endsWith("ches")) {
      return word.substring(0, word.length() - 2);
    }

    if (word.endsWith("s") && word.length() > 3 && !word.endsWith("ss")) {
      return word.substring(0, word.length() - 1);
    }

    return word;
  }

  /**
   * Checks for whole-phrase presence using whitespace boundary padding.
   * <p>
   * Technique: Boundary-padded substring search Related to: Knuth-Morris-Pratt (KMP), Boyer-Moore,
   * regex \b word boundary
   * <p>
   * Why padding instead of regex \b: \b uses Unicode word boundary rules which can behave
   * unexpectedly with medical abbreviations (e.g. "t2d", "ckd", "ibs"). Manual space padding gives
   * explicit, predictable boundary control.
   * <p>
   * Example: text = "i have cod liver oil allergy" phrase = "cod" Without padding: "cod" found
   * inside "cod liver" ✓ but also inside "coconut" ✗ With padding:    " cod " found in " i have cod
   * liver oil allergy " ✓ correct
   * <p>
   * TODO: Consider pre-compiling boundary-padded patterns per keyword at startup
   *       using Pattern.compile() and caching them in a Map<String, Pattern>.
   *       For large keyword dictionaries this avoids repeated string concatenation
   *       on every extract() call.
   */
  private boolean containsWholePhrase(String text, String phrase) {
    return (" " + text + " ").contains(" " + phrase + " ");
  }

  /**
   * Determines whether a token is close enough to a keyword to be a match.
   * <p>
   * Strategy (standard fuzzy search pattern): 1. Exact equality          → O(n) — cheapest, checked
   * first 2. Length difference guard → O(1) — rejects obviously different lengths 3. Levenshtein
   * distance    → O(n×m) — most expensive, checked last
   * <p>
   * This ordering follows the fail-fast principle used in search engine query optimisation —
   * expensive operations only run when cheap ones pass.
   * <p>
   * TODO: Add Jaro-Winkler distance as an alternative path for tokens
   *       shorter than 6 characters. For short medical abbreviations
   *       (ibs, ckd, t2d, htn) Levenshtein with threshold 2 is too permissive
   *       (distance between "ibs" and "its" is only 1 — false positive risk).
   *       Jaro-Winkler handles short strings more conservatively.
   * <p>
   * TODO: Consider adding a minimum token length guard (e.g. skip tokens
   *       shorter than 3 characters entirely) to avoid matching stop words
   *       and single-character inputs against medical keywords.
   */
  private boolean isLikelyMatch(String token, String keyword) {
    if (token.equals(keyword)) {
      return true;
    }

    if (Math.abs(token.length() - keyword.length()) > FUZZY_THRESHOLD) {
      return false;
    }

    return levenshtein(token, keyword) <= FUZZY_THRESHOLD;
  }

  /**
   * Builds searchable n-gram tokens from normalised text.
   * <p>
   * Algorithm: N-Gram Tokenisation (standard NLP technique)
   * <p>
   * Produces: - Unigrams  (n=1): individual words - Bigrams   (n=2): adjacent word pairs - Trigrams
   * (n=3): adjacent word triples
   * <p>
   * Why n-grams are necessary here: Medical conditions are frequently multi-word phrases. Without
   * bigrams/trigrams, "type 2 diabetes" would only produce tokens ["type", "2", "diabetes"] — none
   * of which alone fuzzy-matches the 3-word keyword "type 2 diabetes" as a unit.
   * <p>
   * N-grams are the foundational building block of: - Language models (n-gram LMs, predecessor to
   * word2vec / transformers) - TF-IDF and BM25 document ranking - Named Entity Recognition (NER)
   * feature extraction - Google's original search index
   * <p>
   * Current limit: trigrams (n=3). Maximum keyword length in the dictionary is 4 words ("chronic
   * obstructive pulmonary disease" — 4 words).
   * <p>
   * TODO: Extend to 4-grams to cover "chronic obstructive pulmonary disease"
   *       and any other 4-word medical phrases in the keyword dictionary.
   *       Formula: add a 4-gram loop for i < words.length - 3.
   * <p>
   * TODO: Consider replacing manual n-gram construction with Apache Lucene's
   *       ShingleFilter which handles arbitrary n-gram sizes and integrates
   *       with the full Lucene analysis pipeline.
   */
  private List<String> buildTokens(String text) {
    String[] words = text.split("\\s+");
    List<String> tokens = new ArrayList<>(Arrays.asList(words));
    // Bigrams
    for (int i = 0; i < words.length - 1; i++) {
      tokens.add(words[i] + " " + words[i + 1]);
    }
    // Trigrams
    for (int i = 0; i < words.length - 2; i++) {
      tokens.add(words[i] + " " + words[i + 1] + " " + words[i + 2]);
    }

    return tokens;
  }

  /**
   * Computes the Levenshtein edit distance between two strings.
   * <p>
   * Algorithm: Levenshtein Distance (Vladimir Levenshtein, 1965) Category:  Dynamic Programming /
   * String Metrics
   * <p>
   * Definition: The minimum number of single-character edits required to transform string 'a' into
   * string 'b', where allowed edits are: - Insertion:    "penut"   → "peanut"   (insert 'a') -
   * Deletion:     "peannut" → "peanut"   (delete 'n') - Substitution: "bmi"     → "ami"
   * (substitute 'b'→'a')
   * <p>
   * Implementation variant: Space-optimised single-row DP Standard Levenshtein requires an
   * (n+1)×(m+1) matrix → O(n×m) space. This implementation uses a single array of length (m+1) +
   * one scalar 'prev' variable, reducing space to O(m) while keeping time complexity at O(n×m).
   * <p>
   * Early-exit optimisation: Strings whose lengths differ by more than FUZZY_THRESHOLD cannot
   * possibly be within the threshold regardless of content, so the full DP computation is skipped
   * entirely.
   * <p>
   * Levenshtein distance is used in: - Spell checkers (Google "did you mean?") - DNA sequence
   * alignment (bioinformatics — Smith-Waterman is a variant) - Git diff algorithms (patience diff
   * uses edit distance concepts) - Elasticsearch fuzzy queries - OCR post-processing error
   * correction
   * <p>
   * TODO: Consider replacing with Damerau-Levenshtein distance which adds
   *       transposition as a fourth edit operation (cost 1 instead of 2).
   *       "dibaetes" → "diabetes" = distance 1 with transposition,
   *       but distance 2 with standard Levenshtein.
   *       Transpositions are the most common human typing error and
   *       Damerau-Levenshtein handles them more accurately.
   *       Reference: Frederick Damerau, 1964.
   */
  private int levenshtein(String a, String b) {
    if (Math.abs(a.length() - b.length()) > FUZZY_THRESHOLD) {
      return FUZZY_THRESHOLD + 1;
    }

    int[] dp = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      dp[j] = j;
    }

    for (int i = 1; i <= a.length(); i++) {
      int prev = dp[0];
      dp[0] = i;

      for (int j = 1; j <= b.length(); j++) {
        int temp = dp[j];
        dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ?
            prev :
            1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
        prev = temp;
      }
    }

    return dp[b.length()];
  }
}

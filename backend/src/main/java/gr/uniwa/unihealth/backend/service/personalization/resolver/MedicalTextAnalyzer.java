package gr.uniwa.unihealth.backend.service.personalization.resolver;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Turns free text — Greek, English, or the two mixed in one sentence — into the normalised tokens
 * the dictionary is keyed on.
 *
 * <p>Deliberately pure and static. It holds no state, touches no Spring bean and reads no database,
 * which is what lets every rule below be exercised in a throwaway script before anything is
 * compiled, and by an ordinary unit test later with no application context.
 *
 * <p>It is also the single seam where a third-party analyser would go. Everything downstream
 * consumes tokens and stems; swapping this class for Lucene's Greek and English filter chains would
 * not touch the dataset, the index or the matcher. That was considered and deferred: Lucene's
 * {@code GreekStemmer} never strips a bare final vowel, so it leaves διαβήτης and διαβήτη in
 * separate buckets — the exact case this needs to unify.
 */
public final class MedicalTextAnalyzer {

  private MedicalTextAnalyzer() {
  }

  /**
   * Below this many characters a word is never stemmed, because there is not enough of it left to
   * be sure the ending is an inflection rather than part of the root.
   */
  private static final int MIN_WORD_LENGTH_TO_STEM = 5;

  /** A suffix that would leave a stem shorter than this is skipped and a shorter one tried. */
  private static final int MIN_STEM_LENGTH = 3;

  /**
   * A lookup key shorter than this is never registered; such a term is matched on its surface form
   * instead.
   *
   * <p>This is the floor that keeps short Greek stems from becoming traps, and it is load-bearing in
   * two ways worth spelling out, because both look like accidents:
   *
   * <ul>
   *   <li>τόνος (tuna) stems to {@code τον}, which is also the Greek definite article. Registering
   *       that stem would make every Greek sentence containing "τον" report a fish allergy.</li>
   *   <li>ΧΑΠ is the Greek abbreviation for COPD and stems to {@code χαπ}. So does χάπια, "pills" —
   *       it is five characters, so it <em>is</em> stemmed. The two only stay apart because
   *       {@code χαπ} is three characters and therefore never becomes a stem key, leaving ΧΑΠ
   *       reachable only by writing it exactly. «παίρνω χάπια» matches nothing, which is correct.</li>
   * </ul>
   */
  public static final int MIN_KEY_LENGTH = 4;

  /** Clause boundaries, marked before punctuation is stripped so the positions survive. */
  private static final Pattern CLAUSE_BREAK = Pattern.compile("[,.;:!?\\u00B7\\n\\r]+");

  /** Keeps Latin, digits, Greek, and the clause marker. Everything else becomes a space. */
  private static final Pattern NON_ALNUM_SPACE = Pattern.compile("[^a-z0-9\\u0370-\\u03FF|\\s]");

  private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

  private static final String CLAUSE_MARKER = "|";

  /**
   * Contrastive conjunctions open a new clause, because they reverse polarity: "no nuts but I eat
   * eggs" must not let the negation reach the second half. Plain "and"/«και» deliberately does not
   * — "no nuts and no eggs" is one negated thought.
   */
  private static final Set<String> CLAUSE_OPENERS = Set.of("αλλα", "ομωσ", "but", "however");

  /**
   * Greek inflectional suffixes, longest first. Inflection only — no derivational endings.
   *
   * <p>{@code -ικός}, {@code -ώδης}, {@code -ίτιδα} and {@code -ισμός} are deliberately absent.
   * Inflection is regular and safe to strip; derivation changes meaning (άσθμα, ασθματικός and
   * αντιασθματικό are three different things) and belongs in the dataset where a human decided it.
   */
  private static final String[] GREEK_SUFFIXES = {
      "ματων", "ματοσ", "ασεωσ",
      "ατοσ", "ατων", "ουσα", "οντα",
      "ιων", "ιεσ", "εων", "ουσ", "ατα", "εισ", "οισ", "αισ",
      "ησ", "ασ", "οσ", "εσ", "ων", "ου", "οι", "ια", "ει", "αι",
      "α", "ο", "η", "ι", "ε", "υ", "ω"
  };

  /** Greek to Latin, digraphs first so «ου» never decomposes into ο + υ. */
  private static final String[][] GREEK_TO_LATIN = {
      {"ου", "u"}, {"ει", "i"}, {"οι", "i"}, {"υι", "i"}, {"αι", "e"},
      {"αυ", "af"}, {"ευ", "ef"}, {"μπ", "b"}, {"ντ", "d"}, {"γκ", "g"},
      {"γγ", "g"}, {"τσ", "ts"}, {"τζ", "tz"},
      {"α", "a"}, {"β", "v"}, {"γ", "g"}, {"δ", "d"}, {"ε", "e"}, {"ζ", "z"},
      {"η", "i"}, {"θ", "th"}, {"ι", "i"}, {"κ", "k"}, {"λ", "l"}, {"μ", "m"},
      {"ν", "n"}, {"ξ", "ks"}, {"ο", "o"}, {"π", "p"}, {"ρ", "r"}, {"σ", "s"},
      {"τ", "t"}, {"υ", "i"}, {"φ", "f"}, {"χ", "x"}, {"ψ", "ps"}, {"ω", "o"}
  };

  /**
   * Applied to both sides after transliteration, folding the many ways Greeks spell Greek in Latin
   * letters onto one form.
   *
   * <p>Order is load-bearing: vowel digraphs first, then consonant digraphs, then single letters.
   * The vowel rules are what let a typed "ksiroi karpoi" meet ξηροί καρποί — the Greek side has
   * already collapsed οι to i, so the Latin side has to collapse "oi" the same way or the two keys
   * never line up.
   */
  private static final String[][] LATIN_FOLD = {
      {"ou", "u"}, {"oi", "i"}, {"ei", "i"}, {"ai", "e"},
      {"ch", "x"}, {"kh", "x"}, {"ph", "f"}, {"ks", "x"},
      {"8", "th"}, {"w", "o"}, {"h", "i"}, {"y", "i"}, {"j", "i"},
      {"q", "k"}, {"c", "k"}, {"b", "v"}
  };

  /** One token of free text, with everything the matcher needs to place and compare it. */
  public record Token(String surface, String stem, String skeleton, int clause, boolean greek) {}

  /**
   * Lowercases, strips accents, folds word-final sigma and reduces punctuation to clause markers.
   *
   * <p>{@link StringUtils#stripAccents} replaces the hand-rolled NFD decomposition that used to
   * live here and compiled a fresh {@code Pattern} on every call. It handles Greek correctly, since
   * ά decomposes to α plus a combining mark: ά→α, ϊ→ι, ΐ→ι, ώ→ω. It also settles the long-standing
   * asymmetry where inbound API strings arrive NFC-normalised but this path assumed NFD — after
   * accent stripping both forms are identical, so the question stops mattering. Please do not
   * "restore" the explicit Normalizer call.
   */
  public static String normalize(String input) {
    if (input == null) {
      return "";
    }

    String marked = CLAUSE_BREAK.matcher(input).replaceAll(" " + CLAUSE_MARKER + " ");

    String normalized = StringUtils.stripAccents(marked).toLowerCase(Locale.ROOT)
        .replace('ς', 'σ');

    normalized = NON_ALNUM_SPACE.matcher(normalized).replaceAll(" ");
    return MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
  }

  /** Normalises, then splits into tokens carrying their clause index. */
  public static List<Token> tokenize(String input) {
    String normalized = normalize(input);
    List<Token> tokens = new ArrayList<>();

    if (normalized.isEmpty()) {
      return tokens;
    }

    int clause = 0;
    for (String word : normalized.split(" ")) {
      if (word.isEmpty()) {
        continue;
      }
      if (CLAUSE_MARKER.equals(word) || CLAUSE_OPENERS.contains(word)) {
        clause++;
        continue;
      }
      tokens.add(toToken(word, clause));
    }

    return tokens;
  }

  /** Analyses one already-normalised word. Used for dictionary terms and for user tokens alike. */
  public static Token toToken(String word, int clause) {
    boolean greek = isGreek(word);
    return new Token(word, stem(word, greek), skeleton(word, greek), clause, greek);
  }

  /**
   * Greek and Latin are disjoint scripts, so routing on the script of the first letter is a
   * decision rather than a guess — it is exactly right every time, with no classifier and no
   * probability anywhere in the pipeline. It also means a sentence can switch language mid-way
   * («Έχω asthma και υπέρταση») and both halves still work, which any document-level language
   * detector would get wrong by construction.
   */
  public static boolean isGreek(String word) {
    for (int i = 0; i < word.length(); i++) {
      char c = word.charAt(i);
      if (Character.isLetter(c)) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.GREEK;
      }
    }
    return false;
  }

  public static String stem(String word, boolean greek) {
    return greek ? greekStem(word) : englishStem(word);
  }

  /**
   * Strips one inflectional suffix, longest match first.
   *
   * <p>A suffix that would leave too short a stem is skipped rather than accepted, and the search
   * continues — so τόνος keeps its {@code οσ} rather than collapsing to a three-letter stem.
   */
  public static String greekStem(String word) {
    if (word.length() < MIN_WORD_LENGTH_TO_STEM) {
      return word;
    }

    for (String suffix : GREEK_SUFFIXES) {
      if (word.endsWith(suffix) && word.length() - suffix.length() >= MIN_STEM_LENGTH) {
        return word.substring(0, word.length() - suffix.length());
      }
    }

    return word;
  }

  /** The original English plural rules, unchanged, but now applied to both sides symmetrically. */
  public static String englishStem(String word) {
    if (word.length() < MIN_WORD_LENGTH_TO_STEM) {
      return word;
    }
    if (word.endsWith("ies")) {
      return word.substring(0, word.length() - 3) + "y";
    }
    if (word.endsWith("es") && !word.endsWith("sses") && !word.endsWith("shes")
        && !word.endsWith("ches")) {
      return word.substring(0, word.length() - 2);
    }
    if (word.endsWith("s") && !word.endsWith("ss")) {
      return word.substring(0, word.length() - 1);
    }
    return word;
  }

  /**
   * Folds a word onto a shared Latin skeleton, so Greek and Greeklish meet in the middle.
   *
   * <p>Transliterating Greeklish back to Greek is not possible — a Latin {@code i} could be ι, η, υ,
   * ει or οι, and picking one is guesswork. Folding both sides onto a deliberately lossy form
   * sidesteps that: διαβήτης and a typed "diavitis" or "diavhths" all arrive at {@code diavitis}.
   *
   * <p>The skeleton does not try to resolve every ambiguity, θ/τ and vowel drift in particular. It
   * does not need to — whatever it misses falls through to the fuzzy tier, and that division of
   * labour is what keeps this to a table rather than a transliteration engine.
   */
  public static String skeleton(String word, boolean greek) {
    String result = word;

    if (greek) {
      for (String[] pair : GREEK_TO_LATIN) {
        result = result.replace(pair[0], pair[1]);
      }
    }

    for (String[] pair : LATIN_FOLD) {
      result = result.replace(pair[0], pair[1]);
    }

    return collapseDoubles(result);
  }

  private static String collapseDoubles(String word) {
    StringBuilder out = new StringBuilder(word.length());
    for (int i = 0; i < word.length(); i++) {
      if (i == 0 || word.charAt(i) != word.charAt(i - 1)) {
        out.append(word.charAt(i));
      }
    }
    return out.toString();
  }

  /** Joins per-word stems into the key a multi-word term is stored under. */
  public static String joinStems(List<Token> tokens, int from, int count) {
    return join(tokens, from, count, Token::stem);
  }

  /** Joins per-word skeletons into the Greeklish key a multi-word term is stored under. */
  public static String joinSkeletons(List<Token> tokens, int from, int count) {
    return join(tokens, from, count, Token::skeleton);
  }

  /** Joins the normalised surface forms, used for terms whose stem is too short to be a key. */
  public static String joinSurfaces(List<Token> tokens, int from, int count) {
    return join(tokens, from, count, Token::surface);
  }

  private static String join(List<Token> tokens, int from, int count, Function<Token, String> part) {
    StringBuilder key = new StringBuilder();
    for (int i = from; i < from + count; i++) {
      if (i > from) {
        key.append(' ');
      }
      key.append(part.apply(tokens.get(i)));
    }
    return key.toString();
  }
}

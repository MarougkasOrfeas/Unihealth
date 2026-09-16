package gr.uniwa.unihealth.data.nhs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns one NHS symptom page into a {@link NhsSnapshot.Symptom}.
 *
 * <p>Two things are read from each page. The first is the {@code application/ld+json} block, a
 * schema.org {@code MedicalWebPage} whose {@code hasPart} entries are tagged with a health aspect
 * ({@code OverviewHealthAspect}, {@code CausesHealthAspect}, and so on) and, for the "when to get
 * help" blocks, a triage {@code identifier} of {@code primary}, {@code urgent} or
 * {@code immediate}. Reading that in preference to scraping headings means the section mapping
 * follows a published vocabulary rather than NHS page furniture.
 *
 * <p>The second is the "symptoms and possible causes" table, which only exists in the markup. It is
 * what makes the advanced search possible: the left cell describes the presentation, the right cell
 * names the cause.
 *
 * @author omaro
 */
@Slf4j
public class NhsSymptomParser {

  private static final String BASE_URL = "https://www.nhs.uk";

  /**
   * Identifies the causes table. The second column header is the reliable signal - captions vary
   * wildly ("What causes excessive or smelly farts", "Possible causes of anal pain") and several
   * pages carry treatment tables whose shape is otherwise identical.
   */
  private static final Pattern CAUSE_HEADER =
      Pattern.compile("possible\\s+(cause|condition)", Pattern.CASE_INSENSITIVE);

  /** Fallback for the handful of causes tables published without a header row. */
  private static final Pattern CAUSE_CAPTION =
      Pattern.compile("what\\s+causes|possible\\s+cause", Pattern.CASE_INSENSITIVE);

  /** Commas, en dashes and semicolons all separate the details in the left-hand cell. */
  private static final Pattern CLAUSE_SEPARATOR = Pattern.compile("[,;–]");

  /**
   * Adjectives that cannot stand on their own as a symptom. Splitting "Sore, blurry or watery eyes"
   * on commas would otherwise produce "Sore" as a tickable item, so a clause that reduces to one of
   * these is folded back into the clause that follows it.
   */
  private static final Set<String> DANGLING_ADJECTIVES = Set.of("sore", "sharp", "itchy", "dull",
      "red", "painful", "sudden", "constant", "severe", "mild", "burning", "aching", "stiff",
      "swollen", "flaky", "sticky", "dry", "watery", "blurry", "heavy", "tight", "weak", "numb",
      "hot", "cold");

  /** Words that carry no meaning when deciding whether a clause is a bare adjective. */
  private static final Set<String> FILLER_WORDS = Set.of("a", "an", "the", "often", "very",
      "usually", "sometimes", "may", "be", "and", "or", "with", "your", "you");

  private final ObjectMapper objectMapper;

  public NhsSymptomParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * @param html the page source
   * @param slug the symptom slug, used for the canonical URL and as the identity of the record
   * @return the parsed symptom, or {@code null} if the page carried no usable JSON-LD
   */
  public NhsSnapshot.Symptom parse(String html, String slug) {
    Document doc = Jsoup.parse(html, BASE_URL);

    JsonNode ld = readJsonLd(doc);
    if (ld == null) {
      log.warn("No JSON-LD found on the page for [{}]; skipping it.", slug);
      return null;
    }

    String title = ld.path("name").asText(null);
    if (title == null || title.isBlank()) {
      log.warn("No title in the JSON-LD for [{}]; skipping it.", slug);
      return null;
    }

    Sections sections = new Sections();
    for (JsonNode part : ld.path("hasPart")) {
      readSection(part, sections);
    }

    List<NhsSnapshot.Cause> causes = parseCauseTables(doc);

    return new NhsSnapshot.Symptom(
        title,
        slug,
        readSynonyms(ld),
        ld.path("url").asText(BASE_URL + "/symptoms/" + slug + "/"),
        readLastReviewed(ld),
        blankToNull(ld.path("description").asText("")),
        sections.overview,
        sections.symptoms,
        sections.selfCareDo,
        sections.selfCareDont,
        sections.seeDoctor,
        sections.urgent,
        sections.emergency,
        sections.treatment,
        sections.causes,
        causes);
  }

  // ---------------------------------------------------------------- JSON-LD

  private JsonNode readJsonLd(Document doc) {
    for (Element script : doc.select("script[type=application/ld+json]")) {
      try {
        JsonNode node = objectMapper.readTree(script.data());
        if ("MedicalWebPage".equals(node.path("@type").asText())) {
          return node;
        }
      } catch (Exception e) {
        log.debug("Ignoring a JSON-LD block that did not parse: {}", e.getMessage());
      }
    }
    return null;
  }

  /** {@code about.alternateName} holds the names a reader is likely to search for instead. */
  private String readSynonyms(JsonNode ld) {
    List<String> names = new ArrayList<>();
    for (JsonNode name : ld.path("about").path("alternateName")) {
      String value = name.asText("").trim();
      if (!value.isEmpty()) {
        names.add(value);
      }
    }
    return names.isEmpty() ? null : String.join(", ", names);
  }

  /** {@code lastReviewed} is an array of the review and the next-review date; take the earlier. */
  private String readLastReviewed(JsonNode ld) {
    JsonNode reviewed = ld.path("lastReviewed");
    if (reviewed.isArray() && !reviewed.isEmpty()) {
      return reviewed.get(0).asText(null);
    }
    return blankToNull(reviewed.asText(""));
  }

  private void readSection(JsonNode part, Sections sections) {
    String aspect = part.path("hasHealthAspect").asText("");
    // The "when to get help" aspects name the urgency themselves, so the level comes from the
    // aspect rather than from the per-element identifier.
    boolean triageAspect = aspect.contains("MedicalHelp") || aspect.contains("SeeDoctor");

    List<String> blocks = new ArrayList<>();

    for (JsonNode element : part.path("hasPart")) {
      String rawText = element.path("text").asText("");
      // The overview aspect leads with the page's own in-page navigation - bare strings like
      // "Causes" or "Self-care" - which would otherwise be prepended to the article. Real content
      // is always wrapped in markup, and across the corpus no unwrapped block runs past a label.
      if (!rawText.contains("<")) {
        continue;
      }
      String text = htmlToLines(rawText);
      if (text.isBlank()) {
        continue;
      }

      String headline = element.path("headline").asText("");

      // "Do" and "Don't" arrive as sibling blocks under the same self-care aspect, so the headline
      // is the only thing that tells them apart.
      if (aspect.endsWith("SelfCareHealthAspect")) {
        if (isDont(headline)) {
          sections.selfCareDont = append(sections.selfCareDont, text);
          continue;
        }
        if (isDo(headline)) {
          sections.selfCareDo = append(sections.selfCareDo, text);
          continue;
        }
      }

      // Each block keeps its own heading. A page can carry two triggers at the same urgency -
      // "See a pharmacist if:" and "See a GP if:" - and filing the second one's bullets under the
      // first one's heading would tell the reader to do the wrong thing.
      //
      // Within a triage aspect only the blocks that carry an identifier are triggers; the rest
      // ("A&E safety messaging") are supporting prose behind an internal label, so that label is
      // dropped and the prose kept.
      boolean isTrigger = !element.path("identifier").asText("").isEmpty();
      blocks.add(!headline.isBlank() && (!triageAspect || isTrigger)
          ? headline + "\n" + text
          : text);
    }

    if (blocks.isEmpty()) {
      return;
    }
    String text = String.join("\n", blocks);

    if (aspect.endsWith("OverviewHealthAspect")) {
      sections.overview = append(sections.overview, text);
    } else if (aspect.endsWith("SymptomsHealthAspect")) {
      sections.symptoms = append(sections.symptoms, text);
    } else if (aspect.endsWith("CausesHealthAspect")) {
      sections.causes = append(sections.causes, text);
    } else if (aspect.endsWith("TreatmentsHealthAspect")) {
      sections.treatment = append(sections.treatment, text);
    } else if (aspect.endsWith("SelfCareHealthAspect")) {
      sections.selfCareDo = append(sections.selfCareDo, text);
    } else if (aspect.contains("MedicalHelpEmergency")) {
      sections.emergency = append(sections.emergency, text);
    } else if (aspect.contains("MedicalHelpUrgent")) {
      sections.urgent = append(sections.urgent, text);
    } else if (aspect.contains("MedicalHelpNonurgent") || aspect.contains("SeeDoctor")) {
      sections.seeDoctor = append(sections.seeDoctor, text);
    }
  }

  private boolean isDo(String headline) {
    return headline.trim().equalsIgnoreCase("do");
  }

  private boolean isDont(String headline) {
    String normalised = headline.trim().toLowerCase(Locale.ROOT).replace("’", "'");
    return normalised.equals("don't") || normalised.equals("do not") || normalised.equals("dont");
  }

  // ------------------------------------------------------------ causes table

  private List<NhsSnapshot.Cause> parseCauseTables(Document doc) {
    List<NhsSnapshot.Cause> causes = new ArrayList<>();

    for (Element table : doc.select("table")) {
      if (!isCausesTable(table)) {
        continue;
      }
      for (Element row : table.select("tr")) {
        Elements cells = row.select("td");
        if (cells.size() < 2) {
          continue;
        }
        String factorsText = tidy(cells.get(0).text());
        String causeText = tidy(cells.get(1).text());
        if (factorsText.isEmpty() || causeText.isEmpty()) {
          continue;
        }
        causes.add(new NhsSnapshot.Cause(factorsText, causeText, splitFactors(factorsText),
            readConditions(cells.get(1))));
      }
    }

    return causes;
  }

  /**
   * A causes table is one whose second column is headed "possible cause(s)" or "possible
   * condition(s)". Tables headed "Treatment" or "What you can do" describe what to do about a cause
   * that is already known and would produce nonsense factors, so they are left alone.
   */
  private boolean isCausesTable(Element table) {
    Elements headers = table.select("th");
    if (headers.size() >= 2) {
      return CAUSE_HEADER.matcher(headers.get(1).text()).find();
    }
    if (headers.isEmpty()) {
      Element caption = table.selectFirst("caption");
      return caption != null && CAUSE_CAPTION.matcher(caption.text()).find();
    }
    return false;
  }

  /**
   * Only properly linked conditions become rows in the conditions A-Z. About a third of the cells
   * name a cause in prose alone ("Bleeding in the anus, bowel or lower gut from injury"), and
   * turning those into A-Z entries would fill it with sentences.
   */
  private List<NhsSnapshot.ConditionRef> readConditions(Element cell) {
    List<NhsSnapshot.ConditionRef> conditions = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();

    for (Element link : cell.select("a[href]")) {
      String url = link.absUrl("href");
      if (!url.contains("/conditions/")) {
        // Links out to /medicines/ and the like are context, not causes.
        continue;
      }
      String name = tidy(link.text());
      if (name.isEmpty() || !seen.add(name.toLowerCase(Locale.ROOT))) {
        continue;
      }
      conditions.add(new NhsSnapshot.ConditionRef(name, url));
    }

    return conditions;
  }

  /**
   * Splits a cell such as "Starts after eating, bringing up food or bitter tasting fluids, feeling
   * full and bloated" into the three details a reader can tick.
   */
  List<String> splitFactors(String cell) {
    List<String> clauses = new ArrayList<>();

    for (String raw : CLAUSE_SEPARATOR.split(cell)) {
      String clause = raw.trim();
      if (clause.isEmpty()) {
        continue;
      }
      String previous = clauses.isEmpty() ? null : clauses.get(clauses.size() - 1);
      if (previous != null && (hasOpenBracket(previous) || endsWithDanglingAdjective(previous))) {
        clauses.set(clauses.size() - 1, previous + ", " + clause);
      } else {
        clauses.add(clause);
      }
    }

    return clauses.stream().map(this::tidy).filter(c -> c.length() >= 3).toList();
  }

  /** True when a split landed inside brackets, e.g. "(or you feel hot" / "cold or shivery)". */
  private boolean hasOpenBracket(String clause) {
    return count(clause, '(') > count(clause, ')');
  }

  private boolean endsWithDanglingAdjective(String clause) {
    // Only the trailing segment matters: "Itchy, flaky" still dangles on "flaky".
    String tail = clause.substring(clause.lastIndexOf(',') + 1);
    List<String> words = new ArrayList<>();
    for (String word : tail.toLowerCase(Locale.ROOT).replaceAll("[^a-z\\s-]", "").split("\\s+")) {
      if (!word.isBlank() && !FILLER_WORDS.contains(word)) {
        words.add(word);
      }
    }
    return words.size() == 1 && DANGLING_ADJECTIVES.contains(words.get(0));
  }

  private int count(String value, char character) {
    return (int) value.chars().filter(c -> c == character).count();
  }

  // ----------------------------------------------------------------- text

  /**
   * Flattens a fragment of NHS HTML into the plain text with {@code "- "} bullets that the existing
   * symptom detail page renders. The frontend interpolates these with {@code &#123;&#123; &#125;&#125;} and splits on
   * newlines, so any markup left in would be shown to the reader as escaped tags.
   */
  String htmlToLines(String html) {
    if (html == null || html.isBlank()) {
      return "";
    }

    List<String> lines = new ArrayList<>();
    for (Element block : Jsoup.parseBodyFragment(html).body().select("p, li, h2, h3, h4")) {
      String text = tidy(block.text());
      if (text.isEmpty()) {
        continue;
      }
      lines.add("li".equals(block.tagName()) ? "- " + text : text);
    }

    if (lines.isEmpty()) {
      return tidy(Jsoup.parseBodyFragment(html).text());
    }
    return String.join("\n", lines);
  }

  /**
   * Stripping inline links leaves gaps in front of punctuation ("Bloody diarrhoea , tummy cramps"),
   * so whitespace is closed up before the text is stored.
   */
  private String tidy(String text) {
    if (text == null) {
      return "";
    }
    return text.replace(' ', ' ')
        .replaceAll("\\s+", " ")
        .replaceAll("\\s+([,.;:)\\]])", "$1")
        .replaceAll("([(\\[])\\s+", "$1")
        .trim();
  }

  private String append(String existing, String addition) {
    if (existing == null || existing.isBlank()) {
      return addition;
    }
    return existing + "\n" + addition;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /** Mutable accumulator for the sections of one page. */
  private static final class Sections {

    private String overview;
    private String symptoms;
    private String causes;
    private String treatment;
    private String selfCareDo;
    private String selfCareDont;
    private String seeDoctor;
    private String urgent;
    private String emergency;
  }
}

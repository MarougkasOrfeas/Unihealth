package gr.uniwa.unihealth.backend.service.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import gr.uniwa.unihealth.backend.model.ReferenceDataVersion;
import gr.uniwa.unihealth.backend.repository.LabelKeywordMappingRepository;
import gr.uniwa.unihealth.backend.repository.ReferenceDataVersionRepository;
import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTermIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads the bilingual medical term dictionary from {@code data/labels/medical-terms.json}.
 *
 * <p>Follows {@link NhsSymptomInitData}: the file is read and parsed once, outside the per-tenant
 * loop, then projected into each tenant's database. It is deliberately read from the classpath
 * rather than the network, so a normal start is offline and every machine ends up with the same
 * rows.
 *
 * <p>What changed, and why it matters: this class used to hold 159 keyword rows as Java source,
 * behind a {@code count() > 0} guard. That guard meant the dictionary could never be corrected once
 * a database had been seeded — editing the list was a silent no-op everywhere it had already run.
 * Loading is now gated on a checksum of the file, so editing the JSON and restarting is the entire
 * workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabelKeywordInitData implements ApplicationRunner {

  private static final String RESOURCE = "data/labels/medical-terms.json";

  private static final Pattern LABEL_CODE = Pattern.compile("^(ALLERGY|CHRONIC)_[A-Z0-9_]+$");

  private final LabelKeywordMappingRepository repository;
  private final ReferenceDataVersionRepository versionRepository;
  private final MedicalTermIndex termIndex;
  private final TenantContext tenantContext;

  /** Configured exactly as {@link NhsSymptomInitData} does, so both snapshots bind alike. */
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Override
  public void run(ApplicationArguments args) {
    MedicalTermDictionary dictionary = readDictionary();
    String checksum = readChecksum();

    validate(dictionary);

    tenantContext.runForEachTenant(tenantId -> seed(tenantId, dictionary, checksum),
        "LabelKeywordInitData.run");
  }

  /**
   * Diverges from {@link NhsSymptomInitData}, which logs a warning and carries on when its snapshot
   * is missing. The severities are not comparable: an empty symptom A-Z is a blank page, whereas an
   * empty term dictionary means every health profile silently resolves to {@code ALLERGY_OTHER} and
   * {@code CHRONIC_OTHER} for every user, with nothing in the UI to suggest anything is wrong.
   * Better to refuse to start.
   */
  MedicalTermDictionary readDictionary() {
    ClassPathResource resource = new ClassPathResource(RESOURCE);
    if (!resource.exists()) {
      throw new IllegalStateException(
          "Missing medical term dictionary at [" + RESOURCE + "]. Free-text health answers cannot "
              + "be interpreted without it.");
    }

    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, MedicalTermDictionary.class);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read the medical term dictionary: "
          + e.getMessage(), e);
    }
  }

  private String readChecksum() {
    try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
      return DigestUtils.md5Hex(in);
    } catch (Exception e) {
      throw new IllegalStateException("Could not checksum the medical term dictionary", e);
    }
  }

  private void seed(String tenantId, MedicalTermDictionary dictionary, String checksum) {
    Optional<ReferenceDataVersion> stored = versionRepository.findById(RESOURCE);

    if (stored.isPresent() && stored.get().getChecksum().equals(checksum)) {
      log.debug("Medical term dictionary unchanged for tenant [{}]", tenantId);
      termIndex.rebuild();
      return;
    }

    List<LabelKeywordMapping> rows = flatten(dictionary);

    log.info("Loading medical term dictionary v{} for tenant [{}]: {} concepts, {} terms",
        dictionary.datasetVersion(), tenantId, dictionary.concepts().size(), rows.size());

    // runForEachTenant already wraps this in a transaction and a ShedLock, so the delete and the
    // insert are atomic per tenant even when several instances start at once.
    repository.deleteAll();
    repository.saveAll(rows);
    versionRepository.save(new ReferenceDataVersion(RESOURCE, checksum, rows.size()));

    termIndex.rebuild();
  }

  /**
   * One row per (term, scope). A concept scoped {@code BOTH} produces two rows, because the matcher
   * only ever considers terms belonging to the field being read.
   */
  List<LabelKeywordMapping> flatten(MedicalTermDictionary dictionary) {
    List<LabelKeywordMapping> rows = new ArrayList<>();

    for (MedicalTermDictionary.Concept concept : dictionary.concepts()) {
      for (MedicalTermDictionary.Term term : concept.terms()) {
        for (String scope : scopesOf(concept)) {
          LabelKeywordMapping row = new LabelKeywordMapping();
          // Lowercased on the way in, so uq_label_keyword_mapping constrains what it appears to.
          row.setKeyword(term.text().toLowerCase(Locale.ROOT));
          row.setLabelCode(concept.labelCode());
          row.setKeywordType(scope);
          row.setLang(term.lang());
          row.setTermType(term.type());
          row.setConceptId(concept.conceptId());
          row.setPriority(concept.priority());
          row.setSource(concept.source());
          row.setActive(true);
          rows.add(row);
        }
      }
    }

    return rows;
  }

  /**
   * The free-text fields a concept may be matched against.
   *
   * <p>{@code BOTH} produces a row per field, because the matcher only ever considers terms
   * belonging to the field being read. It exists for coeliac, gluten and lactose, which students
   * routinely write in the allergy box.
   */
  private List<String> scopesOf(MedicalTermDictionary.Concept concept) {
    return "BOTH".equals(concept.scope())
        ? List.of("ALLERGY", "CHRONIC")
        : List.of(concept.scope());
  }

  /**
   * Fails the start on anything that would produce wrong labels rather than no labels.
   *
   * <p>A null priority is the sharpest of these: label sorting unboxes that value, so a single
   * missing number would throw on every profile save rather than degrading quietly.
   */
  void validate(MedicalTermDictionary dictionary) {
    List<String> problems = new ArrayList<>();
    Set<String> seenCodes = new HashSet<>();
    Set<String> seenConcepts = new HashSet<>();
    Map<String, String> seenTerms = new HashMap<>();

    for (MedicalTermDictionary.Concept concept : dictionary.concepts()) {
      String at = concept.conceptId() + "/" + concept.labelCode();

      if (!LABEL_CODE.matcher(concept.labelCode()).matches()) {
        problems.add(at + ": malformed label code");
      }
      // Two concepts sharing a label code would silently merge, and the one seeded second would
      // overwrite the other's priority.
      if (!seenCodes.add(concept.labelCode())) {
        problems.add(at + ": duplicate label code");
      }
      if (!seenConcepts.add(concept.conceptId())) {
        problems.add(at + ": duplicate concept id");
      }
      if (concept.priority() == null || concept.priority() <= 0) {
        problems.add(at + ": priority must be a positive number");
      }
      if (concept.terms() == null || concept.terms().isEmpty()) {
        problems.add(at + ": no terms");
        continue;
      }
      if (concept.terms().stream().noneMatch(term -> "en".equals(term.lang()))) {
        problems.add(at + ": no English term");
      }
      if (concept.terms().stream().noneMatch(term -> "el".equals(term.lang()))) {
        problems.add(at + ": no Greek term");
      }

      for (MedicalTermDictionary.Term term : concept.terms()) {
        if (term.text() == null || term.text().isBlank()) {
          problems.add(at + ": blank term");
          continue;
        }
        if (term.text().length() > 100) {
          problems.add(at + ": term \"" + term.text() + "\" exceeds the keyword column width");
        }

        // Mirrors uq_label_keyword_mapping (keyword, keyword_type, lang). Caught here so a
        // duplicated term reports which two concepts clash, rather than failing mid-insert with a
        // constraint violation naming neither.
        for (String scope : scopesOf(concept)) {
          String key = term.text().toLowerCase(Locale.ROOT) + "|" + scope + "|" + term.lang();
          String owner = seenTerms.putIfAbsent(key, at);
          if (owner != null) {
            problems.add(at + ": term \"" + term.text() + "\" (" + scope + "/" + term.lang()
                + ") is already claimed by " + owner);
          }
        }
      }
    }

    if (!problems.isEmpty()) {
      throw new IllegalStateException("Invalid medical term dictionary:\n  "
          + String.join("\n  ", problems));
    }
  }
}

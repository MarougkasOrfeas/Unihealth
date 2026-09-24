package gr.uniwa.unihealth.backend.service.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.Condition;
import gr.uniwa.unihealth.backend.model.DataSource;
import gr.uniwa.unihealth.backend.model.SymptomCause;
import gr.uniwa.unihealth.backend.model.SymptomFactor;
import gr.uniwa.unihealth.backend.model.SymptomItem;
import gr.uniwa.unihealth.backend.repository.ConditionRepository;
import gr.uniwa.unihealth.backend.repository.DataSourceRepository;
import gr.uniwa.unihealth.backend.repository.SymptomCauseRepository;
import gr.uniwa.unihealth.backend.repository.SymptomFactorRepository;
import gr.uniwa.unihealth.backend.repository.SymptomItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Seeds the symptom corpus from the committed NHS snapshot.
 *
 * <p>Reads {@code resources/data/nhs/nhs-symptoms.json} rather than the network, so a normal start
 * is offline and the resulting database is identical on every machine. Refreshing the snapshot is a
 * separate, explicit act - see {@code data/nhs-ingest}.
 *
 * @author omaro
 */
@Slf4j
@Component
// Ordered only so that AiCorpusInitData, which embeds these rows, is guaranteed to run afterwards.
// Without an explicit order every ApplicationRunner sits at LOWEST_PRECEDENCE and the relative
// order is undefined, which would let the corpus be embedded from an empty table on a first boot -
// and the result would look like a working application with an assistant that cites nothing.
@Order(10)
@RequiredArgsConstructor
public class NhsSymptomInitData implements ApplicationRunner {

  private static final String SNAPSHOT_RESOURCE_NAME = "data/nhs/nhs-symptoms.json";

  private final SymptomItemRepository symptomRepository;
  private final SymptomFactorRepository factorRepository;
  private final SymptomCauseRepository causeRepository;
  private final ConditionRepository conditionRepository;
  private final DataSourceRepository dataSourceRepository;
  private final TenantContext tenantContext;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Override
  public void run(ApplicationArguments args) {
    NhsSnapshot snapshot = readSnapshot();
    if (snapshot == null) {
      return;
    }

    tenantContext.runForEachTenant(tenantId -> {
      if (symptomRepository.count() == 0) {
        log.info("Initializing NHS symptom content for tenant [{}].", tenantId);
        seed(snapshot);
      } else {
        log.info("Symptoms already exist for tenant [{}]. Skipping initialization.", tenantId);
      }
    }, "NhsSymptomInitData.run");
  }

  private NhsSnapshot readSnapshot() {
    ClassPathResource resource = new ClassPathResource(SNAPSHOT_RESOURCE_NAME);
    if (!resource.exists()) {
      log.warn("No NHS snapshot at [{}]; the symptom A-Z will be empty. Run the ingestion once to "
          + "create it.", SNAPSHOT_RESOURCE_NAME);
      return null;
    }

    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, NhsSnapshot.class);
    } catch (Exception e) {
      log.error("Could not read the NHS snapshot: {}", e.getMessage(), e);
      return null;
    }
  }

  private void seed(NhsSnapshot snapshot) {
    DataSource source = saveSource(snapshot.source());

    // Conditions are shared across symptoms, so they are resolved once and reused.
    Map<String, Condition> conditionsBySlug = new HashMap<>();
    int symptomOrder = 1;
    int causeRows = 0;
    int factorCount = 0;

    for (NhsSnapshot.Symptom symptom : snapshot.symptoms()) {
      SymptomItem item = saveSymptom(symptom, source, symptomOrder++);
      List<SymptomCause> causes = saveCauses(symptom, item, source, conditionsBySlug);
      causeRows += causes.size();
      factorCount += applyFactorWeights(causes);
    }

    log.info("Seeded {} symptoms, {} cause rows, {} factors and {} conditions from the NHS "
            + "snapshot retrieved on {}.",
        snapshot.symptoms().size(), causeRows, factorCount, conditionsBySlug.size(),
        snapshot.source().retrievedOn());
  }

  private DataSource saveSource(NhsSnapshot.Source definition) {
    DataSource source = dataSourceRepository.findByCode(definition.code()).orElseGet(DataSource::new);

    source.setCode(definition.code());
    source.setName(definition.name());
    source.setUrl(definition.url());
    source.setLicence(definition.licence());
    source.setLicenceUrl(definition.licenceUrl());
    source.setAttributionText(definition.attributionText());
    source.setLogoUrl(definition.logoUrl());
    source.setRetrievedOn(definition.retrievedOn());
    source.setDisplayOrder(1);
    source.setActive(true);

    return dataSourceRepository.save(source);
  }

  private SymptomItem saveSymptom(NhsSnapshot.Symptom symptom, DataSource source, int order) {
    SymptomItem item = new SymptomItem();

    item.setTitle(symptom.title());
    item.setSlug(symptom.slug());
    item.setStartingLetter(startingLetter(symptom.title()));
    item.setBrief(truncate(symptom.brief(), 1000));
    item.setOverviewText(symptom.overviewText());
    item.setSymptomsText(symptom.symptomsText());
    item.setDoText(symptom.doText());
    item.setDontText(symptom.dontText());
    item.setSeeDoctorIfText(symptom.seeDoctorIfText());
    item.setUrgentText(symptom.urgentText());
    item.setEmergencyText(symptom.emergencyText());
    item.setTreatmentText(symptom.treatmentText());
    item.setCausesText(symptom.causesText());
    item.setSynonyms(truncate(symptom.synonyms(), 1000));
    item.setSource(source);
    item.setSourceUrl(symptom.sourceUrl());
    item.setSourceLastReviewed(parseDate(symptom.lastReviewed()));
    item.setHasFactors(!symptom.causes().isEmpty());
    item.setDisplayOrder(order);
    item.setActive(true);

    return symptomRepository.save(item);
  }

  private List<SymptomCause> saveCauses(NhsSnapshot.Symptom symptom, SymptomItem item,
      DataSource source, Map<String, Condition> conditionsBySlug) {

    // One factor per distinct clause within a symptom, so ticking it matches every cause row that
    // mentions it.
    Map<String, SymptomFactor> factorsByCode = new LinkedHashMap<>();
    List<SymptomCause> saved = new ArrayList<>();
    int causeOrder = 1;

    for (NhsSnapshot.Cause cause : symptom.causes()) {
      SymptomCause entity = new SymptomCause();
      entity.setSymptomItem(item);
      entity.setFactorsText(truncate(cause.factorsText(), 2000));
      entity.setCauseText(truncate(cause.causeText(), 2000));
      entity.setDisplayOrder(causeOrder++);

      Set<SymptomFactor> factors = new LinkedHashSet<>();
      for (String label : cause.factors()) {
        String code = slugify(label);
        if (code.isEmpty()) {
          continue;
        }
        SymptomFactor factor = factorsByCode.get(code);
        if (factor == null) {
          factor = saveFactor(item, code, label, factorsByCode.size() + 1);
          factorsByCode.put(code, factor);
        }
        factors.add(factor);
      }
      entity.setFactors(factors);

      Set<Condition> conditions = new LinkedHashSet<>();
      for (NhsSnapshot.ConditionRef ref : cause.conditions()) {
        Condition condition = resolveCondition(ref, source, conditionsBySlug);
        if (condition != null) {
          conditions.add(condition);
        }
      }
      entity.setConditions(conditions);

      saved.add(causeRepository.save(entity));
    }

    return saved;
  }

  private SymptomFactor saveFactor(SymptomItem item, String code, String label, int order) {
    SymptomFactor factor = new SymptomFactor();

    factor.setSymptomItem(item);
    factor.setCode(truncate(code, 200));
    factor.setLabel(truncate(capitalise(label), 500));
    factor.setWeight(1.0);
    factor.setDisplayOrder(order);

    return factorRepository.save(factor);
  }

  /**
   * Weights a factor by how well it separates the causes of its symptom: a detail listed under
   * every cause says nothing, one listed under a single cause is decisive.
   *
   * <p>In practice the NHS rarely repeats the same wording across rows of one table, so almost
   * every weight comes out at 1.0 and the ranking reduces to plain coverage. The calculation is
   * kept because it is correct whenever the corpus does repeat itself, and it costs nothing.
   *
   * @return the number of factors weighted
   */
  private int applyFactorWeights(List<SymptomCause> causes) {
    Map<String, Integer> occurrences = new HashMap<>();
    Map<String, SymptomFactor> factors = new LinkedHashMap<>();

    for (SymptomCause cause : causes) {
      for (SymptomFactor factor : cause.getFactors()) {
        factors.putIfAbsent(factor.getCode(), factor);
        occurrences.merge(factor.getCode(), 1, Integer::sum);
      }
    }

    for (Map.Entry<String, SymptomFactor> entry : factors.entrySet()) {
      SymptomFactor factor = entry.getValue();
      factor.setWeight(1.0 / occurrences.get(entry.getKey()));
      factorRepository.save(factor);
    }

    return factors.size();
  }

  /**
   * Conditions are identified by name rather than by the last segment of their URL, because NHS
   * links are not consistent about that - the anxiety page ends in {@code /overview/}, which would
   * collide with anything else structured the same way.
   */
  private Condition resolveCondition(NhsSnapshot.ConditionRef ref, DataSource source,
      Map<String, Condition> cache) {

    String slug = slugify(ref.name());
    if (slug.isEmpty()) {
      return null;
    }

    Condition cached = cache.get(slug);
    if (cached != null) {
      return cached;
    }

    Condition condition = new Condition();
    condition.setName(truncate(capitalise(ref.name()), 300));
    condition.setSlug(truncate(slug, 300));
    condition.setStartingLetter(startingLetter(ref.name()));
    condition.setSourceUrl(truncate(ref.url(), 500));
    condition.setSource(source);
    condition.setDisplayOrder(cache.size() + 1);
    condition.setActive(true);

    Condition saved = conditionRepository.save(condition);
    cache.put(slug, saved);
    return saved;
  }

  // ------------------------------------------------------------------ helpers

  private String slugify(String value) {
    if (value == null) {
      return "";
    }
    String normalised = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return truncate(normalised, 200);
  }

  private String startingLetter(String value) {
    if (value == null || value.isBlank()) {
      return "#";
    }
    char first = Character.toUpperCase(value.trim().charAt(0));
    return Character.isLetter(first) ? String.valueOf(first) : "#";
  }

  /** The NHS writes cause names mid-sentence, so they arrive uncapitalised about half the time. */
  private String capitalise(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String trimmed = value.trim();
    return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
  }

  private LocalDateTime parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value).toLocalDateTime();
    } catch (Exception e) {
      log.debug("Ignoring an unparseable review date [{}].", value);
      return null;
    }
  }

  private String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}

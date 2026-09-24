package gr.uniwa.unihealth.backend.service.ai.ingest;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.model.ReferenceDataVersion;
import gr.uniwa.unihealth.backend.repository.ReferenceDataVersionRepository;
import gr.uniwa.unihealth.backend.repository.SymptomItemRepository;
import gr.uniwa.unihealth.backend.repository.UnihealthAssistantItemRepository;
import gr.uniwa.unihealth.backend.service.ai.retrieval.SymptomResolver;
import gr.uniwa.unihealth.backend.service.ai.retrieval.TenantVectorStoreRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds or loads each tenant's AI vector store at startup.
 *
 * <p>Rebuilds the index only when the corpus or embedding model changes.
 * Embedding runs outside tenant transactions because it can take several minutes.
 *
 * @author omaro */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class AiCorpusInitData implements ApplicationRunner {

  private static final String RESOURCE_PREFIX = "ai/embeddings/";

  /** Number of chunks processed before logging progress. */
  private static final int PROGRESS_BATCH = 25;

  private final SymptomItemRepository symptomRepository;
  private final UnihealthAssistantItemRepository assistantRepository;
  private final ReferenceDataVersionRepository versionRepository;
  private final TenantVectorStoreRegistry registry;
  private final SymptomResolver symptomResolver;
  private final TenantContext tenantContext;
  private final AiProperties aiProperties;

  @Value("${spring.ai.ollama.embedding.options.model:unknown}")
  private String embeddingModelName;

  /** Describes whether a tenant's vector store needs to be rebuilt. */
  private record Plan(List<Document> documents, String fingerprint, boolean upToDate) {}

  @Override
  public void run(ApplicationArguments args) {
    Map<String, Plan> plans = new ConcurrentHashMap<>();

    // Read and prepare each tenant's corpus.
    tenantContext.runForEachTenant(
        tenantId -> plans.put(tenantId, plan()), "AiCorpusInitData.read");

    // Build vector stores outside the tenant transaction.
    Map<String, String> embedded = new LinkedHashMap<>();
    plans.forEach((tenantId, plan) -> build(tenantId, plan).ifPresent(
        fingerprint -> embedded.put(tenantId, fingerprint)));

    if (embedded.isEmpty()) {
      return;
    }

    // Record successfully built indexes.
    tenantContext.runForEachTenant(tenantId -> Optional.ofNullable(embedded.get(tenantId))
        .ifPresent(fingerprint -> versionRepository.save(
            new ReferenceDataVersion(resourceKey(), fingerprint,
                plans.get(tenantId).documents().size()))),
        "AiCorpusInitData.record");
  }

  private Plan plan() {
    List<Document> documents = new ArrayList<>();

    symptomRepository.findByActiveTrueOrderByStartingLetterAscDisplayOrderAscTitleAsc()
        .forEach(symptom -> {
          documents.addAll(SymptomChunker.chunk(symptom));

          // Register English symptom names for lookup.
          symptomResolver.registerEnglish(symptom.getSlug(), symptom.getTitle(),
              symptom.getSynonyms());
        });

    assistantRepository.findByActiveTrueOrderBySectionAscDisplayOrderAsc()
        .forEach(item -> AssistantItemChunker.chunk(item).ifPresent(documents::add));

    String fingerprint = CorpusFingerprint.of(documents);
    boolean upToDate = versionRepository.findById(resourceKey())
        .map(version -> fingerprint.equals(version.getChecksum()))
        .orElse(false);

    return new Plan(documents, fingerprint, upToDate);
  }

  /** Loads an existing index or builds a new one when necessary. */
  private Optional<String> build(String tenantId, Plan plan) {
    if (plan.documents().isEmpty()) {
      log.warn("Tenant [{}] has no AI corpus to index. The assistant will answer without "
          + "grounding.", tenantId);
      return Optional.empty();
    }

    File file = registry.fileFor(aiProperties.getVectorStorePath(), tenantId, embeddingModelName);

    if (plan.upToDate() && file.isFile() && load(tenantId, file)) {
      return Optional.empty();
    }

    return embed(tenantId, plan, file);
  }

  private boolean load(String tenantId, File file) {
    try {
      SimpleVectorStore store = registry.newStore();
      store.load(file);
      registry.register(tenantId, store);

      log.info("Loaded the AI corpus for tenant [{}] from {}, skipping re-embedding.",
          tenantId, file.getName());
      return true;
    } catch (Exception e) {
      // Rebuild if the saved index cannot be loaded.
      log.warn("Could not load {} for tenant [{}] ({}). Re-embedding instead.",
          file.getName(), tenantId, e.getMessage());
      return false;
    }
  }

  private Optional<String> embed(String tenantId, Plan plan, File file) {
    int total = plan.documents().size();

    log.info("Embedding {} AI corpus chunks for tenant [{}] with model [{}]. On CPU this takes "
            + "minutes and keeps Ollama busy; the assistant will say so rather than queue behind "
            + "it. Done once - the result is written to {}.",
        total, tenantId, embeddingModelName, file.getName());

    // Flagged for the whole embed, so a chat turn arriving meanwhile can answer in a second
    // instead of waiting behind every outstanding embedding and timing out.
    registry.startedIndexing(tenantId);
    long startedAt = System.currentTimeMillis();

    try {
      SimpleVectorStore store = registry.newStore();

      // Process in batches so progress is visible.
      List<Document> documents = plan.documents();
      for (int from = 0; from < total; from += PROGRESS_BATCH) {
        int to = Math.min(from + PROGRESS_BATCH, total);
        store.add(documents.subList(from, to));

        log.info("Embedded {}/{} chunks for tenant [{}] ({}s elapsed).",
            to, total, tenantId, (System.currentTimeMillis() - startedAt) / 1000);
      }

      registry.register(tenantId, store);
      persist(store, file);

      log.info("Indexed {} chunks for tenant [{}] in {}s.",
          total, tenantId, (System.currentTimeMillis() - startedAt) / 1000);
      return Optional.of(plan.fingerprint());
    } catch (Exception e) {
      log.warn("Could not embed the AI corpus for tenant [{}]: {}. The assistant will answer "
          + "without grounding until the next restart.", tenantId, e.getMessage());
      return Optional.empty();
    } finally {
      registry.finishedIndexing(tenantId);
    }
  }

  private void persist(SimpleVectorStore store, File file) {
    try {
      File directory = file.getParentFile();
      if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
        log.warn("Could not create {}; the index will be rebuilt on every restart.", directory);
        return;
      }
      store.save(file);
    } catch (Exception e) {
      log.warn("Could not write {} ({}). The index is in memory but will be rebuilt on the next "
          + "restart.", file, e.getMessage());
    }
  }

  private String resourceKey() {
    return RESOURCE_PREFIX + embeddingModelName;
  }
}

package gr.uniwa.unihealth.backend.service.ai.retrieval;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One vector store per tenant, held in memory and persisted to disk.
 *
 * <p><b>Why per tenant, and why this is not one bean.</b> The corpus is not the same for every
 * university: {@code t_unihealth_assistant_item} holds each tenant's own FAQ, services and
 * emergency contacts, seeded into its own database. A single shared {@code VectorStore} bean - the
 * obvious thing to write - would answer a {@code uniwa_c2} student from {@code uniwa_c1}'s content,
 * and would do it silently, because a plausible-looking answer is exactly what retrieval returns
 * when it reaches the wrong corpus. Keying by tenant is the same shape
 * {@code MedicalTermIndex.byTenant} already uses, for the same reason.
 *
 * <p><b>Why in memory.</b> The corpus chunks to roughly 565 documents; at 1024 dimensions that is
 * about 2.3 MB of vectors per tenant. Exact brute-force cosine over that is sub-millisecond, so an
 * approximate index buys nothing - and pgvector in particular would fight this application's
 * routing datasource, whose schema initialisation runs with no tenant bound. The JSON file is not a
 * cache to read through on each query; it exists only so a restart loads in milliseconds instead of
 * re-embedding for a minute.
 *
 * <p>The honest cost: this needs a writable directory, and each application instance holds its own
 * copy, so a horizontally scaled deployment re-embeds per node. Acceptable at this size. Moving to
 * a shared store later is a change to this one class, which is the point of the indirection.
 *
 * @author omaro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantVectorStoreRegistry {

  private final EmbeddingModel embeddingModel;
  private final TenantContext tenantContext;

  private final Map<String, SimpleVectorStore> byTenant = new ConcurrentHashMap<>();

  /**
   * Tenants whose corpus is being embedded right now.
   *
   * <p>The first boot against an empty cache embeds 565 chunks one at a time, which on a CPU takes
   * minutes and saturates Ollama completely. A chat turn during that window is not slow, it is
   * doomed: it waits behind every outstanding embedding, blows the Ollama read timeout, and the
   * student watches "writing my answer" for five minutes before an error. Knowing that indexing is
   * in progress lets the turn say so in one second instead.
   */
  private final Set<String> indexing = ConcurrentHashMap.newKeySet();

  /**
   * @return the store for the tenant of the request in flight, or empty when that tenant has no
   *         corpus indexed - which is what happens when Ollama was unreachable at boot. Callers
   *         must treat empty as "answer without grounding", never as an error.
   */
  public Optional<SimpleVectorStore> current() {
    return Optional.ofNullable(byTenant.get(tenantContext.getCurrentTenant()));
  }

  /** @return an empty store, not yet registered. */
  public SimpleVectorStore newStore() {
    return SimpleVectorStore.builder(embeddingModel).build();
  }

  /** Publishes a built store. Replacement is wholesale, so readers never see a half-built index. */
  public void register(String tenantId, SimpleVectorStore store) {
    byTenant.put(tenantId, store);
  }


  /** Marks a tenant as being embedded. Must be paired with {@link #finishedIndexing(String)}. */
  public void startedIndexing(String tenantId) {
    indexing.add(tenantId);
  }

  public void finishedIndexing(String tenantId) {
    indexing.remove(tenantId);
  }

  /**
   * @return true while this request's tenant is still having its corpus embedded, in which case
   *         calling the model would queue behind every outstanding embedding and time out.
   */
  public boolean isIndexing() {
    return indexing.contains(tenantContext.getCurrentTenant());
  }

  /**
   * Where a tenant's vectors are persisted.
   *
   * <p>The embedding model name is part of the filename, not only of the checksum. Two models
   * produce vectors in incomparable spaces, and a file written by one and loaded by another
   * produces no error at all - just silently meaningless similarity scores. Separate filenames make
   * that impossible rather than merely unlikely.
   */
  public File fileFor(String directory, String tenantId, String embeddingModelName) {
    String safeModel = embeddingModelName.replaceAll("[^A-Za-z0-9._-]", "_");
    return new File(directory, "vectors-" + tenantId + "-" + safeModel + ".json");
  }
}

package gr.uniwa.unihealth.backend.service.ai.retrieval;

import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import gr.uniwa.unihealth.backend.service.ai.AiTurnProgress;
import gr.uniwa.unihealth.backend.service.ai.AiTurnStage;
import gr.uniwa.unihealth.backend.service.ai.external.ExternalGate;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedWebRetriever;
import gr.uniwa.unihealth.backend.service.ai.ingest.Urgency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the passages a question should be answered from.
 *
 * <p>Two stages, both deterministic and both before the model:
 *
 * <ol>
 *   <li>{@link SymptomResolver} names the symptom, if the question names one. A resolved slug
 *       becomes a metadata filter, so the search runs over that page's sections rather than over
 *       the whole corpus.</li>
 *   <li>The resolved symptom's urgent and emergency sections are appended unconditionally.</li>
 * </ol>
 *
 * <p>That second stage is the important one, and it is not an optimisation.
 * {@code SymptomItem.emergencyText} is documented on the entity as "always shown, never ranked
 * away", because that is how the symptom pages behave in the UI. Cosine similarity knows nothing
 * about that rule: a student describing chest discomfort in their own gentle words produces a query
 * whose nearest neighbours are the reassuring sections, and the "call 999" passage loses on score
 * precisely when it matters most. Re-applying the editorial rule here is what stops retrieval from
 * quietly contradicting the rest of the product.
 *
 * @author omaro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthRetriever {

  /**
   * How many urgency passages may be force-included. A symptom has at most one urgent and one
   * emergency section, so this is a guard against a corpus change rather than a tuning knob.
   */
  private static final int MAX_URGENCY_PASSAGES = 4;

  private final TenantVectorStoreRegistry registry;
  private final SymptomResolver symptomResolver;
  private final AiProperties aiProperties;
  private final ExternalGate externalGate;
  private final TrustedWebRetriever trustedWebRetriever;

  /**
   * @param passages              the retrieved passages: local first, then any external ones.
   * @param slug                  the symptom the question resolved to, if any.
   * @param grounded              false when no corpus was available at all, which the caller must
   *                              tell the model about rather than letting it answer from training
   *                              data unannounced.
   * @param consultedAuthorities  true when a live lookup ran and returned something, so the client
   *                              can say the answer includes information newer than the library.
   * @param libraryPreparing      true while the corpus is still being embedded, in which case the
   *                              caller must not call the model at all.
   */
  public record Retrieval(List<Document> passages, Optional<String> slug, boolean grounded,
                          boolean consultedAuthorities, boolean libraryPreparing) {

    public static Retrieval ungrounded() {
      return new Retrieval(List.of(), Optional.empty(), false, false, false);
    }

    /**
     * The corpus is mid-embed, so the model must not be called at all this turn.
     *
     * <p>Distinct from {@link #ungrounded()}, which means "answer anyway, just without citations".
     * Here answering anyway is not an option: Ollama is saturated and the request would die of a
     * read timeout several minutes later.
     */
    public static Retrieval preparing() {
      return new Retrieval(List.of(), Optional.empty(), false, false, true);
    }

    /**
     * True when any passage came from outside.
     *
     * <p>Read by the chat service to decide whether mutating tools may be bound this turn. It is
     * the control that stands between a hostile page and a student's account settings, so it is
     * derived from the passages themselves rather than from a flag someone could forget to set.
     */
    public boolean hasExternal() {
      return passages.stream().anyMatch(passage -> GroundedContext.isExternal(passage));
    }
  }

  public Retrieval retrieve(String question, AiTurnProgress progress) {
    // Reported before the store is even looked up. Reporting it afterwards meant that on the one
    // run where it matters most - a first boot with no index yet - the student saw the turn jump
    // straight from "thinking" to "writing my answer", which looked like the stages were broken
    // when in fact the library simply was not there.
    progress.report(AiTurnStage.SEARCHING_LIBRARY);

    if (registry.isIndexing()) {
      // The corpus is still being embedded. Calling the model now would queue behind every
      // outstanding embedding, exhaust the Ollama read timeout and fail after minutes, so the turn
      // stops here and says so instead.
      log.info("The corpus is still being embedded; answering that the library is not ready.");
      return Retrieval.preparing();
    }

    Optional<SimpleVectorStore> store = registry.current();

    if (store.isEmpty()) {
      log.debug("No vector store for this tenant; answering without grounding.");
      return Retrieval.ungrounded();
    }

    Optional<String> slug = symptomResolver.resolve(question);

    // Keyed by document id so a passage force-included for urgency is not also listed by the
    // similarity search. Insertion-ordered, so the model sees the best match first.
    Map<String, Document> passages = new LinkedHashMap<>();

    similar(store.get(), question, slug).forEach(document ->
        passages.putIfAbsent(document.getId(), document));

    slug.ifPresent(resolved -> urgency(store.get(), question, resolved).forEach(document ->
        passages.putIfAbsent(document.getId(), document)));

    List<Document> local = new ArrayList<>(passages.values());

    log.debug("Retrieved {} local passages for a question resolved to [{}].",
        local.size(), slug.orElse("no symptom"));

    // Stage 5b. Appended after the local passages, never interleaved: the curated corpus leads,
    // and a live page only ever supplements it.
    List<Document> external = consultAuthorities(question, local, slug.isPresent(), progress);
    List<Document> found = new ArrayList<>(local);
    found.addAll(external);

    return new Retrieval(found, slug, true, !external.isEmpty(), false);
  }

  /**
   * Asks the trusted authorities, but only when {@link ExternalGate} says the question has earned
   * it.
   *
   * <p>The gate is consulted before the retriever rather than inside it so that "does this leave
   * the machine" stays a single decision in a single place, rather than something distributed
   * across a call chain where it could be accidentally satisfied.
   */
  private List<Document> consultAuthorities(String question, List<Document> local,
      boolean symptomResolved, AiTurnProgress progress) {

    // A question is health-topical when it named a symptom we know, or when the corpus recognised
    // enough of it to return anything at all.
    boolean healthTopical = symptomResolved || !local.isEmpty();

    if (!externalGate.shouldConsultAuthorities(question, local, healthTopical)) {
      return List.of();
    }

    // Reported only here, after the gate has actually opened. Announcing it before the decision
    // would tell the student we are going online on the nine turns in ten where we do not, which
    // would make every other stage message untrustworthy too.
    progress.report(AiTurnStage.SEARCHING_ONLINE);

    try {
      List<Document> external = trustedWebRetriever.retrieve(question, progress).stream()
          .map(candidate -> ExternalPassages.toDocument(candidate))
          .toList();

      log.info("Consulted trusted authorities: {} passages added.", external.size());
      return external;

    } catch (Exception e) {
      // Belt to the retriever's braces. Nothing about an unreachable public website should be able
      // to fail a student's health question.
      log.warn("External lookup failed ({}); answering from the local corpus.", e.getMessage());
      return List.of();
    }
  }

  private List<Document> similar(SimpleVectorStore store, String question, Optional<String> slug) {
    SearchRequest.Builder request = SearchRequest.builder()
        .query(question)
        .topK(aiProperties.getTopK())
        .similarityThreshold(aiProperties.getSimilarityThreshold());

    slug.ifPresent(resolved -> request.filterExpression(
        AiMetadata.SLUG + " == '" + resolved + "'"));

    List<Document> found = search(store, request.build());

    // A filter that matched nothing means the resolver named a symptom the corpus does not hold -
    // possible after a snapshot refresh drops a page. Falling back to the unfiltered search is
    // better than answering with nothing, and the log line is what makes the drift visible.
    if (found.isEmpty() && slug.isPresent()) {
      log.warn("No passages for symptom [{}]; retrying without the filter.", slug.get());
      return search(store, SearchRequest.builder()
          .query(question)
          .topK(aiProperties.getTopK())
          .similarityThreshold(aiProperties.getSimilarityThreshold())
          .build());
    }

    return found;
  }

  /** The resolved symptom's urgent and emergency sections, whatever they scored. */
  private List<Document> urgency(SimpleVectorStore store, String question, String slug) {
    return search(store, SearchRequest.builder()
        .query(question)
        .topK(MAX_URGENCY_PASSAGES)
        // Every urgency passage, not only the ones that happened to score well - that is the whole
        // point of the rule.
        .similarityThresholdAll()
        .filterExpression(AiMetadata.SLUG + " == '" + slug + "' && "
            + AiMetadata.URGENCY + " != '" + Urgency.NONE.name() + "'")
        .build());
  }

  private List<Document> search(SimpleVectorStore store, SearchRequest request) {
    try {
      List<Document> found = store.similaritySearch(request);
      return found == null ? List.of() : found;
    } catch (Exception e) {
      // Embedding the query needs Ollama. If it is down mid-session the assistant should degrade to
      // an ungrounded answer rather than return an error page for a question it could still
      // partially help with.
      log.warn("Similarity search failed ({}); continuing without those passages.", e.getMessage());
      return List.of();
    }
  }
}

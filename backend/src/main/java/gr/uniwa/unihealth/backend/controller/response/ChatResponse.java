package gr.uniwa.unihealth.backend.controller.response;

import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import gr.uniwa.unihealth.backend.dto.ai.PendingActionDTO;

import java.util.List;

/**
 * One assistant turn.
 *
 * @param reply          the generated answer, or {@code null} when {@code safetyKey} is set.
 * @param safetyKey      a lexicon key the client renders instead of {@code reply}, set when the
 *                       message was screened as a crisis and never reached the model. A key rather
 *                       than text so the wording stays reviewed, translatable and identical every
 *                       time - the one response in this feature that must not vary between turns.
 *                       It is also the flag that stops the turn being stored at all.
 * @param citations      the passages the answer was grounded in. Built from document metadata,
 *                       never from the model's own prose, because a model asked to write its own
 *                       citations will eventually invent a plausible URL.
 * @param grounded       whether the answer used the library at all. False means it came from the
 *                       model's training data, which the client should say rather than leave the
 *                       student to assume the answer is sourced.
 * @param pendingAction  a change awaiting the student's explicit approval, or {@code null}. When
 *                       this is set the client must render the card and trust it over the reply
 *                       text: a small model will sometimes announce a change it has only proposed.
 * @param conversationId the chat this turn belongs to, so a client that started a new one learns
 *                       its id. Null when nothing was stored, which is every screened turn.
 */
public record ChatResponse(String reply, String safetyKey, List<CitationDTO> citations,
                           boolean grounded, PendingActionDTO pendingAction,
                           String conversationId) {

  /** The screened case: the model was not called at all, and nothing is stored. */
  public static ChatResponse safety(String safetyKey) {
    return new ChatResponse(null, safetyKey, List.of(), false, null, null);
  }

  /**
   * The same turn, now knowing which conversation it was filed under.
   *
   * <p>Exists because the id is decided after the answer: persistence happens in the controller,
   * once it is known that the turn reached the model and is therefore storable at all.
   */
  public ChatResponse withConversationId(String id) {
    return new ChatResponse(reply, safetyKey, citations, grounded, pendingAction, id);
  }
}

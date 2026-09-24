package gr.uniwa.unihealth.backend.service.ai.ingest;

/**
 * How urgent the advice in a chunk is.
 *
 * <p>Carried in document metadata so the retriever can honour a rule the symptom pages already
 * follow in the UI: {@code SymptomItem.emergencyText} is documented there as "always shown, never
 * ranked away". Cosine similarity has no notion of that, and a student describing a symptom in
 * their own words can easily produce a query whose nearest chunks are all reassuring. Tagging
 * urgency is what lets retrieval reproduce the editorial rule instead of quietly contradicting it.
 *
 * @author omaro
 */
public enum Urgency {

  /** Ordinary informational content. */
  NONE,

  /** NHS "ask for an urgent GP appointment or get advice from 111 now". */
  URGENT,

  /** NHS "call 999 or go to A&amp;E now". */
  EMERGENCY
}

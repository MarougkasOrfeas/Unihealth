package gr.uniwa.unihealth.backend.service.ai.safety;

/**
 * How severe a screened message is, and therefore what the turn does instead of answering.
 *
 * <p>Declared most severe first: when a message matches terms from more than one tier, the first
 * constant wins. Someone who writes about both self-harm and chest pain is answered about the
 * self-harm.
 *
 * @author omaro
 */
public enum CrisisTier {

  /**
   * The student may be about to hurt themselves. Nothing generated is good enough here, so the turn
   * ends with reviewed text carrying the Greek crisis lines.
   */
  SELF_HARM,

  /**
   * Signs that need emergency care now - chest pain, breathing difficulty, loss of consciousness.
   * Also short-circuits: a model that spends four paragraphs on differential causes of chest pain
   * before mentioning 112 has already failed.
   */
  MEDICAL_EMERGENCY
}

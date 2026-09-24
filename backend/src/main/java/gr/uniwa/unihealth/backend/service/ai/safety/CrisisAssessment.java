package gr.uniwa.unihealth.backend.service.ai.safety;

/**
 * What {@link CrisisGuard} found in a message, and what the turn should say instead of answering.
 *
 * @param tier       how severe the match was.
 * @param messageKey the lexicon key of the reviewed response. A key rather than text, because the
 *                   reply has to follow the user's language switch exactly as {@code
 *                   ai.chat.greeting} already does, and because text chosen here could not be
 *                   translated by the people who own the wording.
 * @param matchedTerm the authored phrase that fired, carried only so the match can be logged and
 *                   argued with. It is never shown to the student: quoting their own crisis words
 *                   back at them is not something this feature should do.
 *
 * @author omaro
 */
public record CrisisAssessment(CrisisTier tier, String messageKey, String matchedTerm) {
}

package gr.uniwa.unihealth.backend.dto.ai;

import java.time.LocalDateTime;

/**
 * One row of the chat-history sidebar.
 *
 * <p>Deliberately without messages. The sidebar shows perhaps twenty conversations and needs a
 * title and a date for each; loading every transcript to render a list would read the student's
 * entire history on every page view.
 *
 * @param id            opaque handle, passed back to continue or open the conversation.
 * @param title         the first question, trimmed.
 * @param lastMessageOn when it was last added to, which is what the list sorts by.
 *
 * @author omaro
 */
public record AiConversationDTO(String id, String title, LocalDateTime lastMessageOn) {
}

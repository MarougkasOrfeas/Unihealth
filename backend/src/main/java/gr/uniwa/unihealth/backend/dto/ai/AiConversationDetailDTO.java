package gr.uniwa.unihealth.backend.dto.ai;

import java.util.List;

/**
 * A conversation with its messages, returned when the student opens one.
 *
 * @param id       opaque handle, sent on the next turn to continue this conversation.
 * @param title    the first question, trimmed.
 * @param messages in order. May contain gaps relative to what the student remembers: a turn
 *                 screened as a crisis was answered but never written, by design.
 *
 * @author omaro
 */
public record AiConversationDetailDTO(String id, String title, List<AiMessageDTO> messages) {
}

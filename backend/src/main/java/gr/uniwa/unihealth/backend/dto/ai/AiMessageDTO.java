package gr.uniwa.unihealth.backend.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a stored chat message.
 *
 * @param role      role message author: USER or ASSISTANT
 * @param content   content message content
 * @param citations citations sources used for the answer
 * @param grounded  whether the answer used library sources
 * @param createdOn when the message was created
 *
 * @author omaro
 */
public record AiMessageDTO(String role, String content, List<CitationDTO> citations,
                           boolean grounded, LocalDateTime createdOn) {
}

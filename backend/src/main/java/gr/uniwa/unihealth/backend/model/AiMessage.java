package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import gr.uniwa.unihealth.backend.model.converter.CitationListConverter;
import gr.uniwa.unihealth.backend.model.enums.AiMessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a stored message in an AI conversation.
 *
 * <p>Messages are immutable and only stored after a request reaches the model.
 *
 * @author omaro
 */
@Getter
@Setter
@Entity
@Table(name = "t_ai_message")
public class AiMessage extends BaseEntity {

  @Column(name = "conversation_id", updatable = false, nullable = false)
  private String conversationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AiMessageRole role;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  /** Sources used to support the answer. */
  @Convert(converter = CitationListConverter.class)
  @Column(columnDefinition = "text")
  private List<CitationDTO> citations;

  @Column(nullable = false)
  private boolean grounded;

  /** Message order within the conversation. */
  @Column(name = "sequence_no", nullable = false)
  private int sequenceNo;
}

package gr.uniwa.unihealth.backend.service.ai.conversation.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.controller.response.ChatResponse;
import gr.uniwa.unihealth.backend.dto.ai.AiConversationDTO;
import gr.uniwa.unihealth.backend.dto.ai.AiConversationDetailDTO;
import gr.uniwa.unihealth.backend.dto.ai.AiMessageDTO;
import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import gr.uniwa.unihealth.backend.model.AiConversation;
import gr.uniwa.unihealth.backend.model.AiMessage;
import gr.uniwa.unihealth.backend.model.enums.AiMessageRole;
import gr.uniwa.unihealth.backend.repository.AiConversationRepository;
import gr.uniwa.unihealth.backend.repository.AiMessageRepository;
import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import gr.uniwa.unihealth.backend.service.ai.conversation.AiConversationService;
import gr.uniwa.unihealth.backend.service.ai.retrieval.SymptomResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation for {@link AiConversationService}.
 *
 * @author omaro
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {

  private static final int MAX_TITLE_LENGTH = 60;
  private static final int MIN_WORD_BOUNDARY = 40;

  private final AiConversationRepository conversationRepository;
  private final AiMessageRepository messageRepository;
  private final SymptomResolver symptomResolver;

  @Override
  public String recordTurn(String userId, String conversationId, String question,
      ChatResponse response) {

    AiConversation conversation = conversationId == null
        ? create(userId, question, response)
        : owned(userId, conversationId);

    int next = messageRepository.countByConversationId(conversation.getId());

    messageRepository.save(message(conversation.getId(), AiMessageRole.USER, question,
        null, false, next + 1));
    messageRepository.save(message(conversation.getId(), AiMessageRole.ASSISTANT,
        response.reply(), response.citations(), response.grounded(), next + 2));
    conversation.setLastMessageOn(LocalDateTime.now());
    conversationRepository.save(conversation);

    return conversation.getId();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AiConversationDTO> findMine(String userId, int limit) {
    return conversationRepository
        .findByUserIdOrderByLastMessageOnDesc(userId, PageRequest.of(0, limit))
        .stream()
        .map(conversation -> new AiConversationDTO(conversation.getId(), conversation.getTitle(),
            conversation.getLastMessageOn()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AiConversationDetailDTO findMine(String userId, String conversationId) {
    AiConversation conversation = owned(userId, conversationId);

    List<AiMessageDTO> messages = messageRepository
        .findByConversationIdOrderBySequenceNoAsc(conversation.getId())
        .stream()
        .map(message -> new AiMessageDTO(message.getRole().name(), message.getContent(),
            message.getCitations() == null ? List.of() : message.getCitations(),
            message.isGrounded(), message.getCreatedOn()))
        .toList();

    return new AiConversationDetailDTO(conversation.getId(), conversation.getTitle(), messages);
  }

  @Override
  public void deleteMine(String userId, String conversationId) {
    conversationRepository.delete(owned(userId, conversationId));
    log.info("Deleted one AI conversation for user {}.", userId);
  }

  @Override
  public void deleteAllMine(String userId) {
    conversationRepository.deleteByUserId(userId);
    log.info("Deleted all AI conversations for user {}.", userId);
  }

  /** Finds a conversation only if it belongs to the given user. */
  private AiConversation owned(String userId, String conversationId) {
    return conversationRepository.findByIdAndUserId(conversationId, userId)
        .orElseThrow(() -> new QDoesNotExistException("Conversation does not exist"));
  }

  private AiConversation create(String userId, String question, ChatResponse response) {
    AiConversation conversation = new AiConversation();

    conversation.setUserId(userId);
    conversation.setTitle(titleFrom(question, response));
    conversation.setLastMessageOn(LocalDateTime.now());

    return conversationRepository.save(conversation);
  }

  /**
   * Creates a title from the detected symptom, grounded source,or the first question as a fallback.
   */
  private String titleFrom(String question, ChatResponse response) {
    return symptomResolver.topicOf(StringUtils.defaultString(question))
        .or(() -> groundedTopic(response))
        .map(StringUtils::capitalize)
        .orElseGet(() -> trimmedQuestion(question));
  }

  /** Finds a conversation topic from its non-live citations. */
  private Optional<String> groundedTopic(ChatResponse response) {
    if (response == null || response.citations() == null) {
      return Optional.empty();
    }

    return response.citations().stream()
        .filter(citation -> !AiMetadata.SOURCE_TYPE_LIVE_WEB.equals(citation.sourceType()))
        .map(CitationDTO::title)
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .flatMap(symptomResolver::topicOf);
  }

  /** Shortens a question so it can be used as a readable conversation title. */
  private String trimmedQuestion(String question) {
    String trimmed = StringUtils.normalizeSpace(StringUtils.defaultString(question));

    if (trimmed.isEmpty()) {
      return "…";
    }

    if (trimmed.length() <= MAX_TITLE_LENGTH) {
      return trimmed;
    }

    String cut = trimmed.substring(0, MAX_TITLE_LENGTH);
    int lastSpace = cut.lastIndexOf(' ');

    return (lastSpace >= MIN_WORD_BOUNDARY ? cut.substring(0, lastSpace) : cut).trim() + "…";
  }

  private AiMessage message(String conversationId, AiMessageRole role, String content,
      List<CitationDTO> citations, boolean grounded, int sequenceNo) {

    AiMessage message = new AiMessage();

    message.setConversationId(conversationId);
    message.setRole(role);
    message.setContent(StringUtils.defaultString(content));
    message.setCitations(citations);
    message.setGrounded(grounded);
    message.setSequenceNo(sequenceNo);

    return message;
  }
}

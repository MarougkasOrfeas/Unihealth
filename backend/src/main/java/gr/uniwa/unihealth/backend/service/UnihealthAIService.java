package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.controller.response.ChatResponse;
import gr.uniwa.unihealth.backend.service.ai.AiTurnProgress;

public interface UnihealthAIService {

  /**
   * Answers one question.
   *
   * <p>Returns a {@link ChatResponse} rather than a bare string because not every turn reaches the
   * model: a message screened as a crisis comes back as a lexicon key instead of generated text.
   *
   * @param userMessage what the student typed.
   * @param progress    where to report each stage as it starts. A turn on this hardware can take
   *                    ten seconds or more, and a silent wait that long reads as a hang. Pass
   *                    {@link AiTurnProgress#NONE} when nobody is listening.
   */
  ChatResponse answer(String userMessage, AiTurnProgress progress);
}

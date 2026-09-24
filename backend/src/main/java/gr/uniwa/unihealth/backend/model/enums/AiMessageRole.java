package gr.uniwa.unihealth.backend.model.enums;

/**
 * Who wrote a stored chat message.
 *
 * <p>Only the two roles a student ever sees. {@code SYSTEM} and {@code TOOL} exist in the prompt
 * but are deliberately not stored: the system prompt is a file under version control, and tool
 * traffic is an implementation detail of one turn. Neither is part of the conversation the student
 * is looking back at, and both would be noise in a transcript they may one day ask to see.
 *
 * @author omaro
 */
public enum AiMessageRole {

  USER,
  ASSISTANT
}

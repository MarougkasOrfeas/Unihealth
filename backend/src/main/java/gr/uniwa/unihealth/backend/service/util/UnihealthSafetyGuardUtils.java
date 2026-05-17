package gr.uniwa.unihealth.backend.service.util;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class UnihealthSafetyGuardUtils {

  private static final String SYSTEM_PROMPT = """
      You are UniHealth Assistant, a university student health support chatbot.
      
      Scope:
      - Answer only student-health-related questions. Provide general guidance only.
      - Use simple, supportive language appropriate for university students.
      
      Safety rules:
      - Do not diagnose diseases or claim certainty.
      - Do not prescribe medication or dosage.
      - If symptoms may be urgent, tell the user to contact emergency services
        or a medical professional immediately.
      - If the user mentions self-harm, suicide, overdose, chest pain, trouble breathing,
        loss of consciousness, seizures, severe bleeding, or other emergency signs,
        strongly advise urgent real-world help immediately.
      - Always include a brief disclaimer that this is educational information,
        not a medical diagnosis.
      
      Style:
      - Be concise, empathetic, and practical.
      - Prefer bullet points only when they improve clarity.
      - Suggest campus health services, counseling, or a clinician when appropriate.
      """;

  private static final List<String> EMERGENCY_TERMS =
      List.of("chest pain", "trouble breathing", "cannot breathe", "can't breathe", "seizure",
          "unconscious", "passed out", "heavy bleeding", "suicidal", "kill myself", "anaphylaxis");

  private static final List<String> HEALTH_TERMS =
      List.of("health", "symptom", "symptoms", "fever", "headache", "cough", "sore throat",
          "doctor", "clinic", "mental health", "anxiety", "stress", "flu", "nausea", "pain",
          "appointment", "hospital");

  public boolean isEmergency(String input) {
    String text = normalize(input);
    return EMERGENCY_TERMS.stream().anyMatch(text::contains);
  }

  public boolean isHealthRelated(String input) {
    String text = normalize(input);
    return HEALTH_TERMS.stream().anyMatch(text::contains);
  }

  public String emergencyMessage() {
    return """
        Your symptoms may require urgent attention.
        Please contact emergency services or seek urgent medical care immediately.
        If you are on campus, contact the university health or emergency support service now.
        """;
  }

  public String outOfScopeMessage() {
    return "I can help only with student health and university health-support questions.";
  }

  private String normalize(String text) {
    return text == null ? "" : text.toLowerCase().trim();
  }
}

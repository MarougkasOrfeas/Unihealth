package gr.uniwa.unihealth.backend.service.ai.conversation;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.controller.response.ChatResponse;
import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import gr.uniwa.unihealth.backend.model.AiConversation;
import gr.uniwa.unihealth.backend.model.AiMessage;
import gr.uniwa.unihealth.backend.model.enums.AiMessageRole;
import gr.uniwa.unihealth.backend.repository.AiConversationRepository;
import gr.uniwa.unihealth.backend.repository.AiMessageRepository;
import gr.uniwa.unihealth.backend.service.ai.conversation.impl.AiConversationServiceImpl;
import gr.uniwa.unihealth.backend.service.ai.retrieval.SymptomResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain Mockito, no Spring context: everything worth asserting here is decided in Java.
 *
 * @author omaro
 */
@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {

  private static final String ME = "user-1";
  private static final String SOMEBODY_ELSE = "user-2";

  @Mock
  private AiConversationRepository conversationRepository;

  @Mock
  private AiMessageRepository messageRepository;

  private AiConversationServiceImpl service;

  @BeforeEach
  void setUp() throws Exception {
    // The real resolver over an inline dataset rather than a mock: naming a conversation is the
    // one thing this class does that is worth asserting end to end, and the test seam exists so
    // it does not have to depend on the 221 aliases that ship with the application.
    SymptomResolver resolver = new SymptomResolver(new ObjectMapper()
        .readTree("{\"aliases\": {\"headaches\": [\"πονοκέφαλος\", \"κεφαλαλγία\"],"
            + " \"dizziness\": [\"ζαλάδα\", \"ίλιγγος\"]}}"));

    // What AiCorpusInitData does at startup, and what lets a citation's English page title be
    // read back as a subject. Without it the grounded fallback below cannot fire at all.
    resolver.registerEnglish("headaches", "Headaches", "Head pain");
    resolver.registerEnglish("dizziness", "Dizziness", "");

    service = new AiConversationServiceImpl(conversationRepository, messageRepository, resolver);
  }

  private static ChatResponse answer() {
    return answerCiting("Headaches", "LOCAL_VETTED");
  }

  /** An answer from the model's own training data, so there is no subject to read off it. */
  private static ChatResponse ungroundedAnswer() {
    return new ChatResponse("Δοκίμασε σταθερό ωράριο ύπνου.", null, List.of(), false, null, null);
  }

  private static ChatResponse answerCiting(String pageTitle, String sourceType) {
    return new ChatResponse("Πιες νερό και ξεκουράσου.", null,
        List.of(new CitationDTO(pageTitle, "Overview", "NHS", "https://www.nhs.uk/x",
            false, sourceType, "", "")),
        true, null, null);
  }

  private AiConversation existing(String id, String owner) {
    AiConversation conversation = new AiConversation();

    conversation.setId(id);
    conversation.setUserId(owner);
    conversation.setTitle("Πονοκέφαλος");
    conversation.setLastMessageOn(LocalDateTime.now().minusDays(1));

    return conversation;
  }

  /** Echoes back whatever was saved, the way a JPA repository does for an already-identified row. */
  private void savesConversationsBack() {
    when(conversationRepository.save(any(AiConversation.class)))
        .thenAnswer(invocation -> {
          AiConversation saved = invocation.getArgument(0);

          if (saved.getId() == null) {
            saved.setId("new-conversation");
          }

          return saved;
        });
  }

  /** The conversation the service handed to the repository, which is where the title is decided. */
  private AiConversation created() {
    ArgumentCaptor<AiConversation> saved = ArgumentCaptor.forClass(AiConversation.class);
    verify(conversationRepository, atLeastOnce()).save(saved.capture());

    return saved.getValue();
  }

  @Test
  @DisplayName("a null conversation id starts a conversation owned by the caller")
  void createsOnFirstTurn() {
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    String id = service.recordTurn(ME, null, "Δεν κοιμάμαι καλά τελευταία", ungroundedAnswer());

    assertThat(id).isEqualTo("new-conversation");
    assertThat(created().getUserId()).isEqualTo(ME);
    assertThat(created().getLastMessageOn()).isNotNull();
  }

  @Test
  @DisplayName("the conversation is named after the symptom the question was about")
  void namesTheConversationAfterItsSubject() {
    // «Πονοκέφαλος» rather than the sentence, because the rail is 272px wide and a student
    // scanning it should not have to read a question to learn what a chat was. The resolver has
    // already matched this question to narrow retrieval, so the name costs a map lookup - no
    // second generation, which on this hardware would be another ten seconds.
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    service.recordTurn(ME, null, "Έχω πονοκέφαλο εδώ και τρεις μέρες, τι να κάνω;", answer());

    assertThat(created().getTitle()).isEqualTo("Πονοκέφαλος");
  }

  @Test
  @DisplayName("the lay word is the name, not the clinical synonym that also matches")
  void prefersTheLayWord() {
    // The alias file authors lay words first because students type them, and the first entry is
    // what becomes the name. «Κεφαλαλγία» would be correct and unhelpful.
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    service.recordTurn(ME, null, "Τι προκαλεί την κεφαλαλγία;", answer());

    assertThat(created().getTitle()).isEqualTo("Πονοκέφαλος");
  }

  @Test
  @DisplayName("a phrasing no alias covers is still named, from what the answer was grounded in")
  void namesFromTheGroundedPassages() {
    // The case that matters. «ζαλίζομαι» is not in the alias file, so matching the question
    // declines - but bge-m3 found the dizziness page anyway, and the citation says so. Reading
    // the subject back off the citation is what stops this being titled with the whole sentence.
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    service.recordTurn(ME, null, "Γιατί ζαλίζομαι όταν σηκώνομαι απότομα;",
        answerCiting("Dizziness", "LOCAL_VETTED"));

    assertThat(created().getTitle()).isEqualTo("Ζαλάδα");
  }

  @Test
  @DisplayName("a live bulletin never names the conversation")
  void ignoresLivePagesWhenNaming() {
    // An ECDC outbreak notice is about an outbreak, not about this student. Titling a chat that
    // merely mentioned travel «Ιλαρά» would be alarming and wrong.
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    service.recordTurn(ME, null, "Χρειάζομαι εμβόλια πριν ταξιδέψω;",
        answerCiting("Headaches", "LIVE_WEB"));

    assertThat(created().getTitle()).isEqualTo("Χρειάζομαι εμβόλια πριν ταξιδέψω;");
  }

  @Test
  @DisplayName("a question naming no symptom keeps the question as its title")
  void fallsBackToTheQuestion() {
    // Most chats land here - sleep, stress, nutrition, exercise name no symptom - and inventing a
    // subject for them would be worse than showing what the student actually typed.
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    service.recordTurn(ME, null, "Πώς διαχειρίζομαι το άγχος των εξετάσεων;", ungroundedAnswer());

    assertThat(created().getTitle()).isEqualTo("Πώς διαχειρίζομαι το άγχος των εξετάσεων;");
  }

  @Test
  @DisplayName("both halves of the turn are stored, in order, with the answer's citations")
  void storesQuestionAndAnswer() {
    savesConversationsBack();
    when(conversationRepository.findByIdAndUserId("c1", ME))
        .thenReturn(Optional.of(existing("c1", ME)));
    // Two messages already there, so this turn continues at 3 and 4 rather than restarting.
    when(messageRepository.countByConversationId("c1")).thenReturn(2);

    service.recordTurn(ME, "c1", "Και τι τον προκαλεί;", answer());

    ArgumentCaptor<AiMessage> messages = ArgumentCaptor.forClass(AiMessage.class);
    verify(messageRepository, times(2)).save(messages.capture());

    AiMessage question = messages.getAllValues().getFirst();
    AiMessage reply = messages.getAllValues().getLast();

    assertThat(question.getRole()).isEqualTo(AiMessageRole.USER);
    assertThat(question.getSequenceNo()).isEqualTo(3);
    assertThat(question.getCitations()).isNull();

    assertThat(reply.getRole()).isEqualTo(AiMessageRole.ASSISTANT);
    assertThat(reply.getSequenceNo()).isEqualTo(4);
    assertThat(reply.isGrounded()).isTrue();
    assertThat(reply.getCitations()).hasSize(1);
  }

  @Test
  @DisplayName("a recorded turn bumps last_message_on, which is what the rail sorts on")
  void bumpsLastMessageOn() {
    AiConversation conversation = existing("c1", ME);
    LocalDateTime before = conversation.getLastMessageOn();

    savesConversationsBack();
    when(conversationRepository.findByIdAndUserId("c1", ME)).thenReturn(Optional.of(conversation));
    when(messageRepository.countByConversationId("c1")).thenReturn(2);

    service.recordTurn(ME, "c1", "Και τι τον προκαλεί;", answer());

    assertThat(conversation.getLastMessageOn()).isAfter(before);
  }

  @Test
  @DisplayName("somebody else's conversation is absent, not forbidden, and nothing is written")
  void refusesAnotherStudentsConversation() {
    // Reported as 404 on purpose: a 403 would confirm that the id exists, which is the one thing
    // an id belonging to another student must never reveal.
    when(conversationRepository.findByIdAndUserId("c1", SOMEBODY_ELSE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.recordTurn(SOMEBODY_ELSE, "c1", "Γεια", answer()))
        .isInstanceOf(QDoesNotExistException.class);

    verify(messageRepository, never()).save(any());
  }

  @Test
  @DisplayName("a long first question is trimmed on a word boundary")
  void trimsTitleOnAWordBoundary() {
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    // Names no symptom, so it takes the fallback path - which is the only path that trims.
    String question = "Πώς μπορώ να βελτιώσω τον ύπνο μου όταν διαβάζω μέχρι αργά "
        + "για τις εξετάσεις;";

    service.recordTurn(ME, null, question, ungroundedAnswer());

    String title = created().getTitle();

    assertThat(title).hasSizeLessThanOrEqualTo(61)  // 60 plus the ellipsis
        .endsWith("…")
        // A mid-word cut reads as a bug rather than as a truncation.
        .doesNotContain(" …");
    assertThat(question).startsWith(title.substring(0, title.length() - 1));
  }

  @Test
  @DisplayName("a title that fits is left exactly as asked")
  void keepsShortTitlesWhole() {
    savesConversationsBack();
    when(messageRepository.countByConversationId(anyString())).thenReturn(0);

    service.recordTurn(ME, null, "  Πονάει   ο λαιμός μου  ", ungroundedAnswer());

    // Normalised, because a title is rendered in a 272px rail where runs of spaces are just gaps.
    assertThat(created().getTitle()).isEqualTo("Πονάει ο λαιμός μου");
  }

  @Test
  @DisplayName("reading somebody else's conversation is absent too")
  void refusesToOpenAnotherStudentsConversation() {
    when(conversationRepository.findByIdAndUserId("c1", SOMEBODY_ELSE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findMine(SOMEBODY_ELSE, "c1"))
        .isInstanceOf(QDoesNotExistException.class);
  }

  @Test
  @DisplayName("deleting somebody else's conversation deletes nothing")
  void refusesToDeleteAnotherStudentsConversation() {
    when(conversationRepository.findByIdAndUserId("c1", SOMEBODY_ELSE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteMine(SOMEBODY_ELSE, "c1"))
        .isInstanceOf(QDoesNotExistException.class);

    verify(conversationRepository, never()).delete(any());
  }
}

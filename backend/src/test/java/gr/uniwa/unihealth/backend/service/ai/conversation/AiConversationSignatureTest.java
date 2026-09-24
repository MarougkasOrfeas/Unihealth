package gr.uniwa.unihealth.backend.service.ai.conversation;

import gr.uniwa.unihealth.backend.repository.AiConversationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes ownership mechanical rather than a habit, in the spirit of {@code AiToolSignatureTest}.
 *
 * <p>{@code FileRepository} states the rule this enforces, and a chat transcript is at least as
 * sensitive as the medical documents it was written for: <i>"find by id" is a question nothing
 * should be able to ask.</i> Loading by (id, owner) rather than by id and then comparing means a
 * row that is not yours is simply not found - the safe answer, and one branch fewer for a caller
 * to forget.
 *
 * <p>The rule is easy to hold for five methods and easy to lose on the sixth, which is why it is
 * asserted by reflection instead of in a review.
 *
 * @author omaro
 */
class AiConversationSignatureTest {

  private static List<Method> declared(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods())
        // Nothing synthetic: the compiler generates bridge methods that carry no parameter names.
        .filter(method -> !method.isSynthetic())
        .toList();
  }

  @Test
  @DisplayName("there are methods to check, so this cannot pass by finding nothing")
  void thereAreMethodsToCheck() {
    // Without this, renaming the interface would turn the whole class into a green no-op.
    assertThat(declared(AiConversationService.class)).isNotEmpty();
    assertThat(declared(AiConversationRepository.class)).isNotEmpty();

    // Every assertion below reads parameter names, which only exist while javac is given
    // -parameters. Spring Boot's parent enables it today; this fails loudly if that changes.
    assertThat(declared(AiConversationService.class)).allSatisfy(method ->
        assertThat(method.getParameters()).allSatisfy(parameter ->
            assertThat(parameter.getName())
                .withFailMessage("Parameter names are not being retained - compile with "
                    + "-parameters, or this whole test class is decorative.")
                .doesNotMatch("arg\\d+")));
  }

  @Test
  @DisplayName("every service method takes the owner first, and never anything else identifying")
  void everyServiceMethodIsScopedToAUser() {
    // First, specifically: it is the parameter a caller is least likely to reorder by accident,
    // and a uniform shape is what makes "the controller resolves this from the principal" a rule
    // rather than a case-by-case decision.
    for (Method method : declared(AiConversationService.class)) {
      Parameter[] parameters = method.getParameters();

      assertThat(parameters)
          .withFailMessage("%s takes no arguments; every method here must be scoped to a user.",
              method.getName())
          .isNotEmpty();

      assertThat(parameters[0].getName())
          .withFailMessage("%s takes '%s' first. The owner must come first on every method, and "
                  + "it must come from the security context, never from the request.",
              method.getName(), parameters[0].getName())
          .isEqualTo("userId");
    }
  }

  @Test
  @DisplayName("every conversation finder names a user, so there is no unscoped lookup to call")
  void everyRepositoryFinderIsScopedToAUser() {
    for (Method method : declared(AiConversationRepository.class)) {
      assertThat(Arrays.stream(method.getParameters()).map(Parameter::getName))
          .withFailMessage("AiConversationRepository.%s does not take a userId. A conversation "
                  + "must only be reachable through its owner: loading by id and comparing "
                  + "afterwards is the branch this rule exists to remove.",
              method.getName())
          .contains("userId");
    }
  }

  @Test
  @DisplayName("nothing on the service is named in a way that suggests it acts for someone else")
  void noMethodOffersToActForAnotherUser() {
    // A findAll or a deleteFor(username) would compile, pass every other test here, and hand one
    // student another's transcripts. This is the one place that notices.
    for (Method method : declared(AiConversationService.class)) {
      assertThat(method.getName())
          .withFailMessage("%s reads as an unscoped or on-behalf-of operation.", method.getName())
          .doesNotStartWith("findAll")
          .doesNotStartWith("deleteAllFor")
          .doesNotContain("ForUser", "ByUsername");
    }
  }
}

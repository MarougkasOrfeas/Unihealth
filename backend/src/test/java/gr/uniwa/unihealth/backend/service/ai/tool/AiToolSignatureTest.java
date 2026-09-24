package gr.uniwa.unihealth.backend.service.ai.tool;

import gr.uniwa.unihealth.backend.service.ai.action.AiActionKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes one security property mechanical instead of a promise in a code review.
 *
 * <p>This application enforces permissions in its controllers. There is no {@code @PreAuthorize}
 * and no {@code @EnableMethodSecurity}; {@code SecurityConfig} says only
 * {@code anyRequest().authenticated()}. A tool calls a service bean directly, so it goes around
 * every one of those checks. The remaining protection is that a tool must never let the model
 * choose <em>whose</em> data it is acting on - identity has to come from the security context.
 *
 * <p>That is easy to hold for four tools and easy to lose on the fifth, particularly since
 * {@code HealthProfileService.updateCurrentUserProfile(String username, ...)} and
 * {@code UserLabelMetricService.record(String username, ...)} both take a username and are exactly
 * the sort of thing a future tool would wrap. So the rule is asserted here over every
 * {@code @Tool} method by reflection, and a new tool that breaks it fails the build.
 */
class AiToolSignatureTest {

  /** Every class exposing {@code @Tool} methods. A new tool class belongs in this list. */
  private static final List<Class<?>> TOOL_CLASSES =
      List.of(HealthLookupTools.class, PreferenceActionTools.class);

  /**
   * Parameter names that would let the model name its subject. Matched on the whole name so that
   * an innocent {@code symptomName} is not caught by "name".
   */
  private static final Pattern FORBIDDEN_PARAMETER =
      Pattern.compile("^(user|username|user_?id|owner|subject|tenant|tenant_?id|email|principal|"
          + "account|account_?id)$", Pattern.CASE_INSENSITIVE);

  /**
   * Types no tool parameter may have. A model cannot be trusted to construct an entity, and a tool
   * accepting one would be accepting whatever the model imagined.
   */
  private static final Set<String> FORBIDDEN_TYPES =
      Set.of("gr.uniwa.unihealth.backend.model.User",
          "org.springframework.security.core.Authentication",
          "org.springframework.security.oauth2.jwt.Jwt");

  private static List<Method> toolMethods() {
    return TOOL_CLASSES.stream()
        .flatMap(type -> List.of(type.getDeclaredMethods()).stream())
        .filter(method -> method.isAnnotationPresent(Tool.class))
        .toList();
  }

  @Test
  @DisplayName("there are tools to check, so this test cannot pass by finding nothing")
  void thereAreToolsToCheck() {
    // Without this, deleting every tool or renaming the annotation would turn the whole class into
    // a green no-op - the classic way a guard test stops guarding.
    assertThat(toolMethods()).isNotEmpty();

    // The name checks below are only meaningful while javac keeps parameter names. Without
    // -parameters they all become "arg0" and every assertion passes for the wrong reason. Spring
    // Boot's parent enables the flag today; this fails loudly if that ever changes. Spring AI needs
    // it too, for the tool schemas it shows the model.
    assertThat(toolMethods()).allSatisfy(method ->
        assertThat(method.getParameters()).allSatisfy(parameter ->
            assertThat(parameter.getName())
                .withFailMessage("Parameter names are not being retained - compile with "
                    + "-parameters, or this whole test class is decorative.")
                .doesNotMatch("arg\\d+")));
  }

  @Test
  @DisplayName("no tool takes a parameter naming whose data to act on")
  void noIdentityParameters() {
    for (Method method : toolMethods()) {
      for (Parameter parameter : method.getParameters()) {
        assertThat(FORBIDDEN_PARAMETER.matcher(parameter.getName()).matches())
            .withFailMessage("Tool %s.%s takes a parameter named '%s'. Identity must come from "
                    + "AuthenticationContext, never from the model: the permission checks this "
                    + "would bypass live in the controllers, not in the services.",
                method.getDeclaringClass().getSimpleName(), method.getName(), parameter.getName())
            .isFalse();
      }
    }
  }

  @Test
  @DisplayName("no tool takes a user, principal or token as a parameter type")
  void noIdentityTypes() {
    for (Method method : toolMethods()) {
      for (Parameter parameter : method.getParameters()) {
        assertThat(FORBIDDEN_TYPES).withFailMessage(
                "Tool %s.%s takes a %s. A model must not be able to construct the identity it "
                    + "acts as.",
                method.getDeclaringClass().getSimpleName(), method.getName(),
                parameter.getType().getName())
            .doesNotContain(parameter.getType().getName());
      }
    }
  }

  @Test
  @DisplayName("every tool describes itself, or the model cannot choose between them")
  void everyToolHasADescription() {
    for (Method method : toolMethods()) {
      assertThat(method.getAnnotation(Tool.class).description())
          .withFailMessage("Tool %s.%s has no description.",
              method.getDeclaringClass().getSimpleName(), method.getName())
          .isNotBlank();
    }
  }

  @Test
  @DisplayName("every mutating tool only proposes; none applies a change itself")
  void mutationsAreProposalsOnly() {
    // The security property this whole feature rests on: no path from the model writes anything.
    // A tool that called updateMyPreferences directly would bypass both the confirmation card and
    // the stale-state check, and would do it without tripping any other test here.
    for (Method method : PreferenceActionTools.class.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Tool.class)) {
        continue;
      }

      assertThat(method.getName())
          .withFailMessage("Tool %s must be named propose*: mutating tools may only create a "
              + "pending action for a human to confirm.", method.getName())
          .startsWith("propose");
    }
  }

  @Test
  @DisplayName("the action catalog contains nothing destructive")
  void actionCatalogIsNotDestructive() {
    // AiActionKind is the entire list of what the assistant may ever propose. Account deletion is
    // absent because the application has no self-service delete path at all; this asserts that the
    // absence stays deliberate.
    for (AiActionKind kind : AiActionKind.values()) {
      assertThat(kind.name())
          .withFailMessage("%s looks destructive for an AI-proposed action.", kind)
          .doesNotContain("DELETE", "REMOVE", "DEACTIVATE", "PURGE");
    }
  }

  @Test
  @DisplayName("no tool name suggests it deletes or deactivates anything")
  void noDestructiveTools() {
    // Account deletion is already impossible: there is no self-service path, and
    // UserServiceImpl.validateForDelete blocks self-deletion, last-admin deletion and the tenant
    // default admin. This test is here so the guarantee cannot be softened by adding a tool later
    // without someone deliberately deleting this assertion.
    for (Method method : toolMethods()) {
      String name = method.getName().toLowerCase(Locale.ROOT);

      assertThat(name).withFailMessage("Tool %s looks destructive. Mutating tools must go through "
              + "a confirmed pending action, and account deletion must stay unreachable.",
          method.getName())
          .doesNotContain("delete", "remove", "deactivate", "destroy", "purge", "drop");
    }
  }

  @Test
  @DisplayName("the read-only tool set stays read-only")
  void readOnlyToolsAreReadOnly() {
    // Everything on HealthLookupTools answers a question. The moment one of them writes, it needs
    // the confirmation handshake instead, and this list is where that shows up.
    for (Method method : HealthLookupTools.class.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Tool.class)) {
        continue;
      }

      String name = method.getName().toLowerCase(Locale.ROOT);

      assertThat(List.of("set", "update", "save", "create", "toggle", "enable", "disable"))
          .withFailMessage("%s on the read-only tool class looks like a mutation.",
              method.getName())
          .noneMatch(name::startsWith);
    }
  }
}

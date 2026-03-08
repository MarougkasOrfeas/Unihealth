package gr.uniwa.unihealth.backend.service.client;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import com.eurodyn.qlack.fuse.lexicon.dto.LanguageDTO;
import com.eurodyn.qlack.fuse.lexicon.service.LanguageService;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.config.properties.KeycloakProperties;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class KeycloakAdminClient {

  private final KeycloakProperties keycloakProperties;

  private final TenantContext tenantContext;

  private final EmailNotificationService emailService;

  private final LanguageService languageService;

  /**
   * Retrieves the Keycloak UserRepresentation by username.
   *
   * @param username the username of the user
   * @return the UserRepresentation if found, null otherwise
   */
  public UserRepresentation getUserRepresentationByUsername(String username) {
    try (Keycloak client = getKeycloakAdminClient()) {
      return client.realm(keycloakProperties.realmUnihealth()).users()
          .searchByUsername(username, true).stream().findFirst().orElse(null);
    }
  }

  /**
   * Retrieves the Keycloak UserRepresentation by email.
   *
   * @param email the email to search for
   * @return the UserRepresentation if found, null otherwise
   */
  public UserRepresentation getUserByEmail(String email) {
    try (Keycloak client = getKeycloakAdminClient()) {
      List<UserRepresentation> users =
          client.realm(keycloakProperties.realmUnihealth()).users().searchByEmail(email, true);
      return users.isEmpty() ? null : users.getFirst();
    }
  }

  /**
   * Checks if a user with the given username exists in Keycloak
   *
   * @param username the username to check
   * @return true if the user exists, false otherwise
   */
  public boolean userExistsByUsername(String username) {
    return getUserRepresentationByUsername(username) != null;
  }

  /**
   * Checks if a user with the given email exists in Keycloak
   *
   * @param email the email to check
   * @return true if the user exists, false otherwise
   */
  public boolean userExistsByEmail(String email) {
    try (Keycloak client = getKeycloakAdminClient()) {
      return !client.realm(keycloakProperties.realmUnihealth()).users().searchByEmail(email, true)
          .isEmpty();
    }
  }

  /**
   * Create the user and stores him in keycloak server
   *
   * @param user The DTO from which the user is created
   */
  public void createKeycloakUser(UserDTO user) {
    LanguageDTO languageDTO = languageService.getLanguage(user.getLanguage());

    UserRepresentation userKeycloak = new UserRepresentation();
    userKeycloak.setUsername(user.getUsername());
    userKeycloak.setEmail(user.getEmail());
    userKeycloak.setFirstName(user.getFirstname());
    userKeycloak.setLastName(user.getLastname());
    userKeycloak.setRequiredActions(Collections.singletonList("VERIFY_EMAIL"));
    userKeycloak.setEnabled(true);
    userKeycloak.setEmailVerified(false);
    userKeycloak.setAttributes(Map.of("locale", List.of(languageDTO.getLocale().toLowerCase()),
        AuthenticationContext.TENANT_CLAIM_NAME, List.of(tenantContext.getCurrentTenant())));

    try (Keycloak client = getKeycloakAdminClient()) {
      Response response =
          client.realm(keycloakProperties.realmUnihealth()).users().create(userKeycloak);
      if (response.getStatus() != 201) {
        Map<String, Object> map = response.readEntity(new GenericType<>() {
        });
        if (map.containsKey("field") && map.get("field").equals("email")) {
          throw ExceptionUtils.createException(QCouldNotSaveException.class, "email_invalid",
              "Email is invalid, couldn't pass Keycloak validator");
        }
        throw ExceptionUtils.createException(IllegalStateException.class,
            "keycloak_user_creation_failed", "Issue with creating user in keycloak (status={})",
            response.getStatus());
      }
    }
  }

  /**
   * Updates the Keycloak user based on the provided UserDTO. Handles email changes according to the
   * user's current status.
   *
   * @param user         The UserDTO containing the updated user information.
   * @param oldEmail     The old email address of the user before the update.
   * @param emailChanged True if the email has been changed, false otherwise. This is used to
   *                     trigger specific logic related to email changes.
   */
  public void updateKeycloakUser(UserDTO user, String oldEmail, boolean emailChanged) {
    LanguageDTO languageDTO = languageService.getLanguage(user.getLanguage());

    try (Keycloak client = getKeycloakAdminClient()) {
      UserRepresentation existingKeycloakUser = getUserRepresentationByUsername(user.getUsername());

      if (existingKeycloakUser == null) {
        throw ExceptionUtils.createException(IllegalStateException.class,
            "keycloak_user_update_failed", "User with username {} does not exist in keycloak",
            user.getUsername());
      }

      UserStatus userDtoStatus = user.getStatus();

      // Update basic user information
      existingKeycloakUser.setFirstName(user.getFirstname());
      existingKeycloakUser.setLastName(user.getLastname());
      existingKeycloakUser.setEmail(user.getEmail());
      existingKeycloakUser.setAttributes(
          Map.of("locale", List.of(languageDTO.getLocale().toLowerCase()),
              AuthenticationContext.TENANT_CLAIM_NAME, List.of(tenantContext.getCurrentTenant())));

      // Handle email change logic
      if (emailChanged) {
        log.info("Email changed for user {} from {} to {}. Current status: {}", user.getUsername(),
            oldEmail, user.getEmail(), user.getStatus());

        // Set email verification to false when email changes
        existingKeycloakUser.setEmailVerified(false);

        // Handle status-specific logic
        switch (userDtoStatus) {
          case ACTIVE:
            handleActiveUserEmailChange(existingKeycloakUser, oldEmail);
            break;
          case UNVERIFIED:
            handleUnverifiedUserEmailChange(client, existingKeycloakUser, oldEmail);
            break;
          case DEACTIVATED:
            handleDeactivatedUserEmailChange(existingKeycloakUser);
            break;
        }
      }
      //ERROR MESSSAGE FOR EMAIL UPDATE WITH INCALID EMAIL
      // Perform the update for keycloak
      client.realm(keycloakProperties.realmUnihealth()).users().get(existingKeycloakUser.getId())
          .update(existingKeycloakUser);

      // Get current status after update
      boolean isEnabled = existingKeycloakUser.isEnabled();
      boolean isVerified = existingKeycloakUser.isEmailVerified();
      UserStatus currentStatus = getUserStatus(isEnabled, isVerified);

      // Send new verification email to new address immediately. Must first complete the update of the user in keycloak to correctly send the verification email.
      // Note: executeActionsEmail automatically expires old verification links
      if (emailChanged && currentStatus.equals(UserStatus.UNVERIFIED)) {
        try {
          client.realm(keycloakProperties.realmUnihealth()).users()
              .get(existingKeycloakUser.getId()).executeActionsEmail(List.of("VERIFY_EMAIL"));
          log.info("Sent verification email to new address {} for user: {}",
              existingKeycloakUser.getEmail(), existingKeycloakUser.getUsername());
        } catch (Exception e) {
          log.error("Failed to send verification email to {} for user: {}",
              existingKeycloakUser.getEmail(), existingKeycloakUser.getUsername(), e);
        }
      }
    } catch (BadRequestException bre) {
      Map<String, Object> map = bre.getResponse().readEntity(new GenericType<>() {
      });
      System.out.println(map);
      if (map.containsKey("field") && map.get("field").equals("email")) {
        throw ExceptionUtils.createException(QCouldNotSaveException.class, "email_invalid",
            "Email is invalid, couldn't pass Keycloak validator");
      }
    }
  }

  /**
   * Deletes a user from Keycloak by their username. Performs the following operations: 1. Looks up
   * the user in Keycloak by username 2. Invalidates all active user sessions (logout) 3. Deletes
   * the user from Keycloak
   *
   * @param username the username of the user to delete
   * @throws IllegalStateException if deletion fails
   */
  public void deleteKeycloakUser(String username) {
    try (Keycloak client = getKeycloakAdminClient()) {
      UserRepresentation userRepresentation = getUserRepresentationByUsername(username);

      if (userRepresentation == null) {
        log.warn("User with username {} does not exist in Keycloak, skipping Keycloak deletion",
            username);
        return;
      }

      String keycloakUserId = userRepresentation.getId();

      // Invalidate all user sessions first (logout the user from all sessions)
      try {
        client.realm(keycloakProperties.realmUnihealth()).users().get(keycloakUserId).logout();
        log.info("Successfully logged out user {} from all Keycloak sessions", username);
      } catch (Exception e) {
        log.warn("Failed to logout user {} from Keycloak sessions: {}", username, e.getMessage());
      }

      // Delete the user from Keycloak
      try (Response response = client.realm(keycloakProperties.realmUnihealth()).users()
          .delete(keycloakUserId)) {

        int status = response.getStatus();
        if (status != 204 && status != 200) {
          throw ExceptionUtils.createException(IllegalStateException.class,
              "keycloak_user_deletion_failed", "Failed to delete user from Keycloak (status={})",
              status);
        }

        log.info("Successfully deleted user {} from Keycloak", username);
      }
    }
  }

  /**
   * Calculates a user's status based on Keycloak state.
   *
   * @param userEnabled  If the user is enabled in Keycloak.
   * @param userVerified If the user's email is verified in Keycloak.
   * @return UserStatus enum value.
   */
  private UserStatus getUserStatus(boolean userEnabled, boolean userVerified) {
    if (!userEnabled) {
      return UserStatus.DEACTIVATED;
    }
    return userVerified ? UserStatus.ACTIVE : UserStatus.UNVERIFIED;
  }

  /**
   * Handles email change for active users. User remains active but cannot log in until new email is
   * verified.
   */
  private void handleActiveUserEmailChange(UserRepresentation keycloakUser, String oldEmail) {
    log.info("Triggered email change for active user: {}", keycloakUser.getUsername());
    prepareUserForEmailVerification(keycloakUser, true);
  }

  /**
   * Handles email change for UNVERIFIED users. Expires existing verification links automatically
   * (Keycloak handles this). Sends informative email to old address. Sends new verification email
   * to new address.
   */
  private void handleUnverifiedUserEmailChange(Keycloak client, UserRepresentation keycloakUser,
      String oldEmail) {
    log.info("Handling email change for unverified user: {}", keycloakUser.getUsername());
    prepareUserForEmailVerification(keycloakUser, true);

    if (!userHasPassword(client, keycloakUser.getId())) {
      List<String> requiredActions = new ArrayList<>(keycloakUser.getRequiredActions() != null ?
          keycloakUser.getRequiredActions() :
          Collections.emptyList());

      if (!requiredActions.contains("UPDATE_PASSWORD")) {
        requiredActions.add("UPDATE_PASSWORD");
        keycloakUser.setRequiredActions(requiredActions);
      }
    }

    String keycloakLocale = keycloakUser.getAttributes().get("locale").getFirst();
    emailService.sendEmailNotification(List.of(oldEmail), keycloakLocale,
        EmailNotificationType.EMAIL_CHANGED);
  }

  private boolean userHasPassword(Keycloak client, String userId) {
    return client.realm(keycloakProperties.realmUnihealth()).users().get(userId).credentials()
        .stream().anyMatch(credential -> "password".equalsIgnoreCase(credential.getType()));
  }

  /**
   * Handles email change for DEACTIVATED users No immediate email is sent When user is reactivated
   * later, verification email will be sent
   */
  private void handleDeactivatedUserEmailChange(UserRepresentation keycloakUser) {
    log.info("Handling email change for deactivated user: {}", keycloakUser.getUsername());
    prepareUserForEmailVerification(keycloakUser, false);
  }

  private void prepareUserForEmailVerification(UserRepresentation keycloakUser, boolean enabled) {
    keycloakUser.setEnabled(enabled);
    keycloakUser.setEmailVerified(false);

    List<String> requiredActions = new ArrayList<>(keycloakUser.getRequiredActions() != null ?
        keycloakUser.getRequiredActions() :
        Collections.emptyList());

    if (!requiredActions.contains("VERIFY_EMAIL")) {
      requiredActions.add("VERIFY_EMAIL");
    }

    keycloakUser.setRequiredActions(requiredActions);
  }

  /**
   * Retrieves user events from Keycloak for a specific user paginated.
   *
   * @param uid        User id
   * @param eventTypes list of event types to filter
   * @param start      how many results to skip
   * @param pageSize   how many results to return
   * @param dateFrom   the start date to filter events
   * @param dateTo     the end date to filter events
   * @return a list of event representations
   */
  public List<EventRepresentation> getKeycloakUserEvents(String uid, List<String> eventTypes,
      int start, int pageSize, String dateFrom, String dateTo) {

    try (Keycloak client = getKeycloakAdminClient()) {
      return client.realm(keycloakProperties.realmUnihealth())
          .getEvents(eventTypes, null, uid, dateFrom, dateTo, null, start, pageSize);
    }
  }

  private Keycloak getKeycloakAdminClient() {
    return KeycloakBuilder.builder().serverUrl(keycloakProperties.serverUrl())
        .realm(keycloakProperties.realmMaster()).username(keycloakProperties.username())
        .password(keycloakProperties.password()).grantType(OAuth2Constants.PASSWORD)
        .clientId(keycloakProperties.clientId()).build();
  }

  public void setUserStatus(String username, boolean enabled) {
    try (Keycloak client = getKeycloakAdminClient()) {
      UserRepresentation userRepresentation = getUserRepresentationByUsername(username);
      UserResource userResource =
          client.realm(keycloakProperties.realmUnihealth()).users().get(userRepresentation.getId());
      userRepresentation.setEnabled(enabled);
      boolean hadVerifyEmailAction =
          userRepresentation.getRequiredActions() != null && userRepresentation.getRequiredActions()
              .contains("VERIFY_EMAIL");
      userResource.update(userRepresentation);
      if (enabled && hadVerifyEmailAction) {
        client.realm(keycloakProperties.realmUnihealth()).users().get(userRepresentation.getId())
            .executeActionsEmail(List.of("VERIFY_EMAIL"));
      }
    }
  }

  public void invalidateUserSessions(String username) {
    try (Keycloak client = getKeycloakAdminClient()) {
      UserRepresentation userRepresentation = getUserRepresentationByUsername(username);
      UserResource userResource =
          client.realm(keycloakProperties.realmUnihealth()).users().get(userRepresentation.getId());
      userResource.logout();
    }
  }

  public boolean isUserVerified(String username) {
    UserRepresentation userRepresentation = getUserRepresentationByUsername(username);
    return userRepresentation.isEmailVerified();
  }
}

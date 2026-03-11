package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QAlreadyExistsException;
import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import com.eurodyn.qlack.fuse.lexicon.dto.LanguageDTO;
import com.eurodyn.qlack.fuse.lexicon.service.LanguageService;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.mapper.UserMapper;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.DeactivationMode;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.UserService;
import gr.uniwa.unihealth.backend.service.client.KeycloakAdminClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation for {@link UserService}.
 *
 * @author omaro
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends BaseUpdatableServiceImpl<UserDTO, User>
    implements UserService {

  private final UserReaderServiceImpl readerService;

  private final UserMapper mapper;

  private final UserRepository userRepository;


  private final LanguageService languageService;

  private final KeycloakAdminClient keycloakAdminClient;

  private final TenantContext tenantContext;

  private final AuthenticationContext authenticationContext;

  @Value("${unihealth.app.users.deactivate.reactivated.after.days}")
  private int deactivateAfterDays;

  /**
   * Initialization method.
   */
  @PostConstruct
  public void init() {
    Map<String, Properties> tenantProperties = tenantContext.getAllTenantProperties();

    tenantContext.runForEachTenant(tenantId -> {
      Properties properties = tenantProperties.get(tenantId);
      String adminUsername = properties.getProperty("admin-username");

      // Skip admin user creation if admin-username is not configured
      if (StringUtils.isEmpty(adminUsername)) {
        log.warn("Skipping admin user creation for tenant '{}' - admin-username not configured",
            tenantId);
        return;
      }

      userRepository.findByUsername(adminUsername).ifPresentOrElse(adminUser -> {
      }, () -> {
        String adminEmail = properties.getProperty("admin-email");
        String adminFirstname = properties.getProperty("admin-firstname");
        String adminLastname = properties.getProperty("admin-lastname");
        String adminLanguage =
            Optional.ofNullable(properties.getProperty("admin-language")).map(String::toLowerCase)
                .orElse("el");

        LanguageDTO languageDTO = languageService.getLanguageByLocale(adminLanguage);

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(adminUsername);
        userDTO.setEmail(adminEmail);
        userDTO.setFirstname(adminFirstname);
        userDTO.setLastname(adminLastname);
        userDTO.setLanguage(languageDTO.getId());
        userDTO.setDeactivationMode(DeactivationMode.AUTOMATIC);
        create(userDTO);
      });
    }, "UserServiceImpl.init");
  }

  @Override
  public String create(UserDTO dto) {
    dto.setUsername(dto.getUsername().toLowerCase());
    dto.setEmail(dto.getEmail().toLowerCase());
    String id = super.create(dto);


    userRepository.flush();
    keycloakAdminClient.createKeycloakUser(dto);

    return id;
  }

  @Override
  public void update(String id, UserDTO dto) {
    // Get existing user to check for email change
    User existingUser = readerService.findEntityById(id, true);
    UserDTO existingUserDTO = mapper.mapToDTO(existingUser);

    String oldEmail = existingUserDTO.getEmail();
    boolean emailChanged = !oldEmail.equalsIgnoreCase(dto.getEmail());

    dto.setEmail(dto.getEmail().toLowerCase());

    super.update(id, dto);
    userRepository.flush();

    userRepository.flush();

    keycloakAdminClient.updateKeycloakUser(dto, oldEmail, emailChanged);
  }

  @Override
  public void delete(String id) {
    User user = readerService.findEntityById(id, true);
    UserDTO userDTO = mapper.mapToDTO(user);
    // Delete user from database
    super.delete(id);

    userRepository.flush();

    // Delete user from Keycloak first
    keycloakAdminClient.deleteKeycloakUser(userDTO.getUsername());

    log.info("Successfully deleted user with id {} and username {}", id, userDTO.getUsername());
  }

  @Override
  public void justLoggedIn(String username) {
    //User keycloak representation from uid.
    UserRepresentation userRepresentation =
        keycloakAdminClient.getUserRepresentationByUsername(username);

    // User DB entity to be updated. Use keycloack user representation to get username to find user in DB.
    User userEntity = userRepository.findByUsername(userRepresentation.getUsername())
        .orElseThrow(() -> new QDoesNotExistException("User does not exist"));
    //Update the status of user account status.
    userEntity.setStatus(
        getUserStatus(userRepresentation.isEnabled(), userRepresentation.isEmailVerified()));

    //In case user was previously deactivated due to prolonged inactivity and then reactivated, they would be deactivated again after 21 days if no login performed.
    userEntity.setDeactivateAfter(null);

    //Retrieve latest login data. Create a timeframe to check.
    keycloakAdminClient.getKeycloakUserEvents(userRepresentation.getId(), List.of("LOGIN"), 0, 1,
        null, null).stream().findFirst().map(EventRepresentation::getTime).ifPresent(timestamp -> {
      LocalDateTime lastLoginTime =
          Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDateTime();
      userEntity.setLastLogin(lastLoginTime);
    });

    userRepository.save(userEntity);
  }

  @Override
  public UserStatus setUserStatus(String id, UserStatus userStatus,
      boolean deactivatedDueToInactivity) {
    return setUserStatus(id, userStatus, deactivatedDueToInactivity, null);
  }

  @Override
  public UserStatus setUserStatus(String id, UserStatus userStatus,
      boolean deactivatedDueToInactivity, String reason) {
    User userToChange = readerService.findEntityById(id, true);

    String currentUser = authenticationContext.getCurrentUsername();
    if (currentUser.equals(userToChange.getUsername())) {
      throw ExceptionUtils.createException(IllegalArgumentException.class,
          "status_change_own_account", "You cannot change the status of your account by yourself.");
    }

    if (UserStatus.DEACTIVATED == userStatus) {
      deactivateUser(userToChange, deactivatedDueToInactivity, reason);
      return UserStatus.DEACTIVATED;
    } else if (UserStatus.ACTIVE == userStatus) {
      return activateUser(userToChange, reason);
    } else {
      throw ExceptionUtils.createException(IllegalArgumentException.class, "unknown_status",
          "User status {0} is currently unknown", userStatus.toString());
    }
  }

  @Override
  public String suggestUsername(String firstname, String lastname) {
    String usernamePrefix = StringUtils.capitalize(firstname.substring(0, 1)) + lastname;
    List<String> availableUsernames = userRepository.findUsernameStartsWith(usernamePrefix);
    if (CollectionUtils.isEmpty(availableUsernames)) {
      return usernamePrefix + ThreadLocalRandom.current().nextInt(0, 10000);
    }
    List<Integer> availableUsernameSuffixes =
        availableUsernames.stream().map(u -> u.split("\\d{4}")).map(s -> Integer.valueOf(s[1]))
            .toList();
    int suffix = ThreadLocalRandom.current().nextInt(0, 10000);
    while (availableUsernameSuffixes.contains(suffix)) {
      suffix = ThreadLocalRandom.current().nextInt(0, 10000);
    }
    return usernamePrefix + suffix;

  }

  @Override
  public boolean checkUsernameExists(String username) {
    Optional<User> userWithUsername = userRepository.findByUsername(username);
    return userWithUsername.isPresent();
  }

  private void deactivateUser(User userToChange, boolean deactivatedDueToInactivity,
      String reason) {
    //    if (userToChange.isGlobalAdmin() && readerService.isUserLastGlobalAdmin(userToChange.getId())) {
    //      log.warn("User with id {} is the last global admin and cannot be deactivated",
    //          userToChange.getId());
    //      return;
    //    }

    userToChange.setStatus(UserStatus.DEACTIVATED);
    userToChange.setDeactivatedDueToInactivity(deactivatedDueToInactivity);
    userToChange.setDeactivateOn(null);
    userToChange.setDeactivationReason(reason);
    userRepository.save(userToChange);
    userRepository.flush();
    keycloakAdminClient.setUserStatus(userToChange.getUsername(), false);
    keycloakAdminClient.invalidateUserSessions(userToChange.getUsername());
  }

  private UserStatus activateUser(User userToChange, String reason) {
    if (keycloakAdminClient.isUserVerified(userToChange.getUsername())) {
      userToChange.setStatus(UserStatus.ACTIVE);
    } else {
      userToChange.setStatus(UserStatus.UNVERIFIED);
    }
    if (userToChange.isDeactivatedDueToInactivity()) {
      userToChange.setDeactivateOn(LocalDateTime.now().plusDays(deactivateAfterDays));
      userToChange.setDeactivatedDueToInactivity(false);
    }
    userToChange.setReactivatedOn(LocalDateTime.now());
    userToChange.setDeactivationMode(DeactivationMode.AUTOMATIC);
    userToChange.setDeactivateAfter(null);
    userToChange.setReactivationReason(reason);
    userRepository.save(userToChange);
    userRepository.flush();
    keycloakAdminClient.setUserStatus(userToChange.getUsername(), true);
    return userToChange.getStatus();
  }

  @Override
  protected BaseReaderServiceImpl<UserDTO, User> getReaderService() {
    return readerService;
  }

  @Override
  protected UserMapper getMapper() {
    return mapper;
  }

  @Override
  protected UserRepository getRepository() {
    return userRepository;
  }

  /**
   * Validates if username and email already exist for new user
   *
   * @param dto UserDTO input from http call.
   */
  @Override
  protected void validateForCreate(UserDTO dto) {
    super.validateForCreate(dto);

    if (keycloakAdminClient.userExistsByUsername(dto.getUsername())) {
      throw ExceptionUtils.createException(QAlreadyExistsException.class, "username_already_exists",
          "Username already exists");
    }

    if (keycloakAdminClient.userExistsByEmail(dto.getEmail())) {
      throw ExceptionUtils.createException(QAlreadyExistsException.class, "email_already_exists",
          "Email already exists");
    }
  }

  @Override
  protected void validateForUpdate(String id, UserDTO dto) {
    super.validateForUpdate(id, dto);
    // Get the existing user by username to get their current email
    User existingUser = userRepository.findByUsername(dto.getUsername())
        .orElseThrow(() -> new QDoesNotExistException("User does not exist"));
    if (!existingUser.getEmail().equalsIgnoreCase(dto.getEmail())) {
      // Check if the new email is already taken by another user
      UserRepresentation userWithSameEmail = keycloakAdminClient.getUserByEmail(dto.getEmail());
      if (userWithSameEmail != null && !userWithSameEmail.getUsername().equals(dto.getUsername())) {
        throw ExceptionUtils.createException(QAlreadyExistsException.class, "email_already_exists",
            "Email already exists");
      }
    }
  }

  @Override
  protected void validateForDelete(String id, User user) {
    // Check if user is a default configuration user created at application startup
    String tenantAdminUsername = tenantContext.getCurrentTenantAdminUsername();
    if (tenantAdminUsername.equalsIgnoreCase(user.getUsername())) {
      log.warn("Cannot delete default admin user with id {} and username {}", id,
          user.getUsername());
      throw ExceptionUtils.createException(IllegalStateException.class,
          "cannot_delete_default_user",
          "Default users added through deployment properties cannot be deleted");
    }

    // Check if user is the last global admin
    //    if (user.isGlobalAdmin() && readerService.isUserLastGlobalAdmin(user.getId())) {
    //      log.warn("Cannot delete user with id {} as they are the last global admin", id);
    //      throw ExceptionUtils.createException(IllegalStateException.class, "cannot_delete_last_admin",
    //          "The last user with the global Admin role cannot be deleted");
    //    }

    // Check if user is trying to delete their own account
    String currentUsername = authenticationContext.getCurrentUsername();
    if (currentUsername != null && currentUsername.equalsIgnoreCase(user.getUsername())) {
      log.warn("User {} attempted to delete their own account", currentUsername);
      throw ExceptionUtils.createException(IllegalStateException.class, "cannot_delete_own_account",
          "User administrators cannot delete their own account");
    }
  }

  //  @Override
  //  public void validate(String id, UserDTO dto) {
  //    if (!CollectionUtils.isEmpty(dto.getGlobalRoles())) {
  //      dto.getGlobalRoles().forEach(roleId -> {
  //        RoleDTO roleDTO = roleReaderService.findById(roleId);
  //        if (!RoleType.GLOBAL.equals(roleDTO.getRoleType())) {
  //          throw ExceptionUtils.createException(QCouldNotSaveException.class,
  //              "invalid_role_type_global", "Only global roles can be assigned to users.");
  //        }
  //      });
  //    }
  //  }

  /**
   * Calculates a user's status.
   *
   * @param userEnabled  If the user is enabled.
   * @param userVerified If the user's email is verified.
   * @return UserStatus enum value.
   */
  private UserStatus getUserStatus(boolean userEnabled, boolean userVerified) {
    if (!userEnabled) {
      return UserStatus.DEACTIVATED;
    }
    return userVerified ? UserStatus.ACTIVE : UserStatus.UNVERIFIED;
  }


}

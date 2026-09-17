package gr.uniwa.unihealth.backend.controller;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.controller.request.SetUserStatusCommand;
import gr.uniwa.unihealth.backend.controller.request.SuggestUsernameCommand;
import gr.uniwa.unihealth.backend.controller.response.SetUserStatusAnswer;
import gr.uniwa.unihealth.backend.dto.RightsMatrix;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import gr.uniwa.unihealth.backend.exception.ExceptionUtils;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.service.*;
import gr.uniwa.unihealth.backend.service.export.ExportService;
import gr.uniwa.unihealth.backend.service.export.UserExportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController extends BaseUpdateableController<UserDTO> {

  private final UserService service;
  private final UserReaderService readerService;
  private final UserExportService exportService;
  private final AuthenticationContext authenticationContext;
  private final UserPreferencesService preferencesService;


  @Override
  public UserDTO findById(String id) {
    if (userPermissionService.userHasGlobalPermission(Permission.ADMIN)) {
      return super.findById(id);
    } else {
      UserDTO loggedInUser = readerService.findLoggedInUser();
      if (loggedInUser.getId().equals(id)) {
        return loggedInUser;
      } else {
        throw userPermissionService.createUnauthorizedException();
      }
    }
  }

  @Override
  public Page<UserDTO> findPage(Map<String, Object> requestBody) {
    Map<String, Object> body = new HashMap<>();
    if (requestBody != null) {
      body.putAll(requestBody);
    }

    if (!userPermissionService.userHasGlobalPermission(Permission.ADMIN)) {
      UserDTO loggedInUser = readerService.findLoggedInUser();
      body.put("id", loggedInUser.getId());
    }

    return readerService.findAll(body);
  }

  @GetMapping("_me")
  @Operation(summary = "Returns the logged in user.",
      description = "Returns the data of the currently authenticated user.")
  public UserDTO getLoggedInUser() {
    return readerService.findLoggedInUser();
  }

  @Operation(summary = "Change the status of a User.",
      description = "Changes the status of a User to either disabled or enabled.")
  @PutMapping("_set_user_status")
  public SetUserStatusAnswer setUserStatus(@RequestBody SetUserStatusCommand setUserStatusCommand) {
    UserStatus userStatus =
        service.setUserStatus(setUserStatusCommand.id(), setUserStatusCommand.userStatus(), false,
            setUserStatusCommand.reason());
    return new SetUserStatusAnswer(userStatus, setUserStatusCommand.reason());
  }

  @Operation(summary = "Should be called after login.",
      description = "Updates the logged in user's data from Keycloak.")
  @PutMapping("_just_logged_in")
  public void justLoggedIn() {
    service.justLoggedIn(authenticationContext.getCurrentUsername());
  }

  @GetMapping("_self/_rights-matrix")
  @Operation(summary = "Returns the rights matrix of the logged in user.",
      description = "Returns the rights matrix of the logged in user.")
  public RightsMatrix getLoggedinUserRightsMatrix() {
    return userPermissionService.getLoggedinUserRightsMatrix();
  }

  /**
   * Self-service, so no ADMIN permission: a student manages their own email preferences. It
   * cannot go through the inherited update, which is admin-guarded and whose mapper ignores most
   * fields anyway.
   */
  @GetMapping("_self/_preferences")
  @Operation(summary = "Returns the email preferences of the logged in user.")
  public UserPreferencesDTO findMyPreferences() {
    return preferencesService.findMyPreferences();
  }

  @PutMapping("_self/_preferences")
  @Operation(summary = "Updates the email preferences of the logged in user.",
      description = "Unsubscribing from the news digest also queues a confirmation email explaining how to subscribe again.")
  public UserPreferencesDTO updateMyPreferences(@RequestBody UserPreferencesDTO dto) {
    return preferencesService.updateMyPreferences(dto);
  }

  @PostMapping("_suggest_username")
  @Operation(summary = "Suggests a username for a user",
      description = "A username is suggested based on first- and lastname of a user who is about to be created.")
  private String suggestUsername(@RequestBody SuggestUsernameCommand suggestUsernameCommand) {
    return service.suggestUsername(suggestUsernameCommand.firstname(),
        suggestUsernameCommand.lastname());
  }

  @GetMapping("_check_username_exists")
  @Operation(summary = "Checks if a username already exists",
      description = "A username provided by the user is checked for existing already for another user record available in the database.")
  private boolean checkUsernameExists(@RequestParam String username) {
    return service.checkUsernameExists(username);
  }

  @GetMapping("_check_email_exists")
  @Operation(summary = "Checks if an email already exists",
      description = "An email provided by the user is checked for existing already for another email record available in the database.")
  private boolean checkEmailExists(@RequestParam String email) {
    return service.checkEmailExists(email);
  }

  @GetMapping("_health_profile_completed")
  @Operation(summary = "Returns whether the logged in user has completed the health profile",
      description = "Returns true if the currently authenticated user has completed the health profile, otherwise false.")
  public boolean isHealthProfileCompleted() {
    return readerService.findLoggedInUser().isHealthProfileCompleted();
  }

  /**
   * Creating a user is an administrative act, and since the request body carries the role, an
   * unguarded endpoint would let any authenticated student ask for {@code ADMIN}. SecurityConfig
   * only requires authentication, so the check belongs here.
   *
   * <p>The deployment administrator created at startup goes through the service directly, so it is
   * unaffected by this guard.
   */
  @Override
  public String create(UserDTO dto) {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return super.create(dto);
  }

  /**
   * Guarded as well as {@link #create}: {@code POST /user/_multiple} reaches the same service and
   * would otherwise be an open side door around the check above.
   */
  @Override
  public Collection<String> createMultiple(List<UserDTO> dtos) {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return super.createMultiple(dtos);
  }

  @GetMapping("{id}/_last_admin")
  @Operation(summary = "Whether this user is the last administrator",
      description = "True when deactivating or deleting this user would leave the application without an administrator who can sign in.")
  public boolean isLastAdmin(@PathVariable String id) {
    userPermissionService.userHasGlobalPermissionOrThrow(Permission.ADMIN);
    return readerService.isLastAdmin(id);
  }

  @Override
  protected BaseUpdatableService<UserDTO> getService() {
    return service;
  }

  @Override
  protected BaseReaderService<UserDTO> getReaderService() {
    return readerService;
  }

  @Override
  protected ExportService<UserDTO> getExportService() {
    return exportService;
  }


  @Override
  protected Collection<Permission> getReadPermissions() {
    return List.of(Permission.ADMIN);
  }
}

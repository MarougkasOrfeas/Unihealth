package gr.uniwa.unihealth.backend.controller;

import com.querydsl.core.types.Predicate;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.controller.request.SetUserStatusCommand;
import gr.uniwa.unihealth.backend.controller.request.SuggestUsernameCommand;
import gr.uniwa.unihealth.backend.controller.response.SetUserStatusAnswer;
import gr.uniwa.unihealth.backend.controller.util.ControllerUtils;
import gr.uniwa.unihealth.backend.dto.UserDTO;
import gr.uniwa.unihealth.backend.model.QUser;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

  private final UserService service;
  private final UserReaderService readerService;
  private final ControllerUtils controllerUtils;
  private final AuthenticationContext authenticationContext;

  @PostMapping
  @Operation(summary = "Creates a new user",
      description = "Creates a new user in the application database.")
  public String create(@RequestBody @Valid UserDTO userDto) {
    return service.create(userDto);
  }

  @Operation(summary = "Updates an existing user",
      description = "Updates an existing user in the application database.")
  @PutMapping("{id}")
  public void update(@PathVariable String id, @RequestBody @Valid UserDTO userDto) {
    service.update(id, userDto);
  }

  @Operation(summary = "Deletes an existing user",
      description = "Deletes an existing user from the application database.")
  @DeleteMapping("{id}")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }

  @Operation(summary = "Finds a single user by id",
      description = "Returns the details of a single user.")
  @GetMapping("{id}")
  public UserDTO findById(@PathVariable String id) {
    return readerService.findById(id);
  }

  @GetMapping("_me")
  @Operation(summary = "Returns the currently logged in user",
      description = "Returns the application user record of the currently authenticated user.")
  public UserDTO findMe() {
    return readerService.findLoggedInUser();
  }

  @Operation(summary = "Finds all Users.",
      description = "Returns the data of the available Users along with pagination information.")
  @PostMapping("_page")
  public Page<UserDTO> findPage(@RequestBody(required = false) Map<String, Object> requestBody) {
    Map.Entry<Predicate, Pageable> predicateAndPageable =
        controllerUtils.getPredicateAndPageable(requestBody, User.class);

    Predicate predicateToUse = predicateAndPageable.getKey();

    UserDTO loggedInUser = readerService.findLoggedInUser();
    predicateToUse = QUser.user.id.eq(loggedInUser.getId()).and(predicateAndPageable.getKey());


    return readerService.findAll(predicateToUse, predicateAndPageable.getValue());
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

  @GetMapping("_health_profile_completed")
  @Operation(summary = "Returns whether the logged in user has completed the health profile",
      description = "Returns true if the currently authenticated user has completed the health profile, otherwise false.")
  public boolean isHealthProfileCompleted() {
    return readerService.findLoggedInUser().isHealthProfileCompleted();
  }
}

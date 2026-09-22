package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.UserSurveyDTO;
import gr.uniwa.unihealth.backend.service.UserSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The «Έρευνα UniHealth» survey response of the logged-in student.
 *
 * <p>Both endpoints are `_me` scoped in the sense {@link TopicFavouriteController} describes: the
 * student is resolved from the authenticated principal and never read from the request, so there is
 * no parameter anyone could set to read or overwrite somebody else's answers.
 *
 * <p>Deliberately not built on {@link BaseController}. Its contract is addressed by id — findById,
 * update(id, dto), _page — which for a one-row-per-student resource would hand out exactly the
 * endpoints this scoping exists to prevent.
 *
 * <p>There is no delete: a response is replaced, never removed. Erasing one is a data-subject
 * request, not a feature, and the row already disappears with the account through the cascade on
 * {@code fk_user_survey_user}.
 */
@RestController
@RequestMapping("survey")
@RequiredArgsConstructor
public class UserSurveyController {

  private final UserSurveyService service;
  private final AuthenticationContext authenticationContext;

  @GetMapping("_me")
  @Operation(summary = "Returns the survey response of the logged in user",
      description = "Answers with an empty response when the user has not taken the survey yet, "
          + "so the form renders blank rather than erroring.")
  public UserSurveyDTO findMySurvey() {
    return service.findForUser(authenticationContext.getCurrentUsername());
  }

  @PutMapping("_me")
  @Operation(summary = "Saves the survey response of the logged in user",
      description = "Replaces any previous response: there is one response per user, and answering "
          + "again overwrites it.")
  public void saveMySurvey(@RequestBody @Valid UserSurveyDTO dto) {
    service.saveForUser(authenticationContext.getCurrentUsername(), dto);
  }
}

package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.HealthProfileViewDTO;
import gr.uniwa.unihealth.backend.dto.UserProfileLabelDTO;
import gr.uniwa.unihealth.backend.service.*;
import gr.uniwa.unihealth.backend.service.personalization.UserProfileLabelsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("profile")
@RequiredArgsConstructor
public class HealthProfileController {

  private final HealthProfileService service;
  private final HealthProfileReaderService readerService;
  private final UserReaderService userReaderService;
  private final UserProfileLabelsService userProfileLabelsService;
  private final AuthenticationContext authenticationContext;

  @PostMapping("_complete")
  @Operation(summary = "Completes the health profile for the logged in user",
      description = "Creates the startup health profile for the currently authenticated user.")
  public String completeProfile(@RequestBody @Valid HealthProfileDTO dto) {
    return service.completeProfile(authenticationContext.getCurrentUsername(), dto);
  }

  @GetMapping("_me")
  @Operation(summary = "Returns the health profile of the logged in user",
      description = "Fetches the health profile of the currently authenticated user.")
  public HealthProfileViewDTO findMyProfile() {
    return readerService.findByCurrentUser(authenticationContext.getCurrentUsername());
  }

  @GetMapping("_me/labels")
  @Operation(summary = "Returns personalization labels for the logged in user",
      description = "Fetches the calculated health-profile labels and their recommendation priorities.")
  public List<UserProfileLabelDTO> findMyLabels() {
    String userId = userReaderService.findLoggedInUser().getId();
    return userProfileLabelsService.findForUser(userId);
  }

  @PutMapping("_me")
  @Operation(summary = "Updates the health profile of the logged in user",
      description = "Updates the health profile of the currently authenticated user.")
  public HealthProfileViewDTO updateMyProfile(@RequestBody @Valid HealthProfileDTO dto) {
    return service.updateCurrentUserProfile(authenticationContext.getCurrentUsername(), dto);
  }
}

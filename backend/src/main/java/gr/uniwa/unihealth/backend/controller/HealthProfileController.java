package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import gr.uniwa.unihealth.backend.dto.HealthProfileViewDTO;
import gr.uniwa.unihealth.backend.service.HealthProfileReaderService;
import gr.uniwa.unihealth.backend.service.HealthProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("profile")
@RequiredArgsConstructor
public class HealthProfileController {

  private final HealthProfileService service;
  private final HealthProfileReaderService readerService;
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

  @PutMapping("_me")
  @Operation(summary = "Updates the health profile of the logged in user",
      description = "Updates the health profile of the currently authenticated user.")
  public HealthProfileViewDTO updateMyProfile(@RequestBody @Valid HealthProfileDTO dto) {
    return service.updateCurrentUserProfile(authenticationContext.getCurrentUsername(), dto);
  }
}

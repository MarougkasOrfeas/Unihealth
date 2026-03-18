package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.OptionalHealthProfileDTO;
import gr.uniwa.unihealth.backend.service.OptionalHealthProfileReaderService;
import gr.uniwa.unihealth.backend.service.OptionalHealthProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("profile")
@RequiredArgsConstructor
public class OptionalHealthProfileController {

  private final OptionalHealthProfileService service;
  private final OptionalHealthProfileReaderService readerService;
  private final AuthenticationContext authenticationContext;

  @GetMapping("_me/optional")
  public OptionalHealthProfileDTO findMyOptionalProfile() {
    return readerService.findByCurrentUser(authenticationContext.getCurrentUsername());
  }

  @PutMapping("_me/optional")
  public void updateMyOptionalProfile(@RequestBody @Valid OptionalHealthProfileDTO dto) {
    service.updateCurrentUserOptionalProfile(authenticationContext.getCurrentUsername(), dto);
  }
}

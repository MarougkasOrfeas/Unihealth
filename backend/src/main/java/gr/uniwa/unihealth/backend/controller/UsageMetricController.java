package gr.uniwa.unihealth.backend.controller;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.LabelUsageDTO;
import gr.uniwa.unihealth.backend.service.UserReaderService;
import gr.uniwa.unihealth.backend.service.personalization.UserLabelMetricService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Opt-in usage measurement per profiling label.
 *
 * Every endpoint is `_me` scoped and resolves the user from the authenticated principal, never
 * from the request. Consent is re-checked in the service, so a submission from a user who has not
 * consented is accepted and discarded rather than trusted.
 */
@RestController
@RequestMapping("usage")
@RequiredArgsConstructor
public class UsageMetricController {

  private final UserLabelMetricService service;
  private final UserReaderService userReaderService;
  private final AuthenticationContext authenticationContext;

  /**
   * Batched submission of engagement deltas.
   *
   * Takes the username straight from the security context rather than going through
   * {@code findLoggedInUser()}: this is the most frequently called endpoint in the application and
   * that path costs a select plus a full DTO mapping on every call.
   */
  @PostMapping("_me")
  @Operation(summary = "Records usage measurements for the logged in user",
      description = "Silently discarded when the user has not consented to measurement.")
  public void recordMyUsage(@RequestBody @Valid List<LabelUsageDTO> usage) {
    service.record(authenticationContext.getCurrentUsername(), usage);
  }

  @GetMapping("_me")
  @Operation(summary = "Returns the usage measurements held for the logged in user",
      description = "Lets the user see exactly what has been recorded about them.")
  public List<LabelUsageDTO> findMyUsage() {
    return service.findForUser(currentUserId());
  }

  @DeleteMapping("_me")
  @Operation(summary = "Deletes all usage measurements held for the logged in user")
  public void deleteMyUsage() {
    service.deleteForUser(currentUserId());
  }

  private String currentUserId() {
    return userReaderService.findLoggedInUser().getId();
  }
}

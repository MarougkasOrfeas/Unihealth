package gr.uniwa.unihealth.backend.service.ai.tool;

import gr.uniwa.unihealth.backend.dto.PossibleCauseDTO;
import gr.uniwa.unihealth.backend.dto.SymptomFactorDTO;
import gr.uniwa.unihealth.backend.dto.SymptomItemDetailDTO;
import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import gr.uniwa.unihealth.backend.dto.ai.SymptomSummaryDTO;
import gr.uniwa.unihealth.backend.service.PossibleCauseService;
import gr.uniwa.unihealth.backend.service.SymptomItemReaderService;
import gr.uniwa.unihealth.backend.service.UserPreferencesService;
import gr.uniwa.unihealth.backend.service.ai.retrieval.SymptomResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The read-only things the assistant may do inside the application.
 *
 * <p>Every method here answers a question; none of them changes anything. That is deliberate for a
 * first increment: a read-only tool needs no confirmation step, no pending-action store and no
 * rollback story, so the model can be given real reach into the application without any of the
 * machinery that mutation would demand.
 *
 * <p><b>No method takes a user, username, tenant or id of any kind.</b> The application's
 * permission checks live in controllers, not services - there is no {@code @PreAuthorize} anywhere
 * - so a tool that called a service with a model-supplied identity would be a way to read another
 * student's data with no check in the way. Identity is resolved from the security context inside
 * the services these tools delegate to, and never crosses the model. {@code AiToolSignatureTest}
 * enforces that mechanically rather than trusting this paragraph.
 *
 * <p>Tool descriptions are terse and imperative on purpose: {@code llama3.1:8b} picks tools poorly
 * from prose, and every extra tool measurably degrades the choice. Five is the ceiling here.
 *
 * @author omaro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthLookupTools {

  private final SymptomItemReaderService symptomReaderService;
  private final PossibleCauseService possibleCauseService;
  private final UserPreferencesService userPreferencesService;
  private final SymptomResolver symptomResolver;

  @Tool(description = "Look up one symptom in the UniHealth library by its name in Greek or "
      + "English. Returns a summary and the advice on when to seek medical help.")
  public SymptomSummaryDTO lookupSymptom(
      @ToolParam(description = "The symptom name, as the student wrote it") String symptomName) {

    Optional<String> slug = symptomResolver.resolve(symptomName);
    if (slug.isEmpty()) {
      log.debug("Tool lookupSymptom could not resolve [{}].", symptomName);
      return null;
    }

    SymptomItemDetailDTO detail = symptomReaderService.findBySlug(slug.get());
    if (detail == null) {
      return null;
    }

    // Trimmed rather than returned whole - see SymptomSummaryDTO. The full page would crowd the
    // context window and silently push the retrieved passages out of it.
    return new SymptomSummaryDTO(detail.getTitle(), detail.getBrief(), detail.getSeeDoctorIfText(),
        detail.getUrgentText(), detail.getEmergencyText(), detail.isHasFactors(), detail.getSlug(),
        detail.getSourceUrl());
  }

  @Tool(description = "List the details a student can confirm about a symptom, to narrow down its "
      + "possible causes. Call this before narrowPossibleCauses.")
  public List<SymptomFactorDTO> listSymptomFactors(
      @ToolParam(description = "The symptom name or slug") String symptom) {

    return slugOf(symptom).map(possibleCauseService::findFactors).orElseGet(List::of);
  }

  /**
   * The one tool worth the round trip.
   *
   * <p>{@code PossibleCauseServiceImpl} already ranks published causes by the share of each one's
   * description the student recognised - matched factor weights over total factor weights, ties
   * broken by how many matched - and returns which factors matched. Those numbers are auditable and
   * come from the NHS table, not from a model.
   *
   * <p>So the assistant is a conversational surface over a transparent algorithm rather than a
   * source of probabilities. That inverts the usual failure: instead of an 8B model inventing "this
   * is probably X", it reads out a published table and says how much of it applied.
   */
  @Tool(description = "Rank the published possible causes of a symptom by how many of its details "
      + "the student recognised. Report the returned scores and matched details exactly as given. "
      + "This is not a diagnosis and you must not describe it as one.")
  public List<PossibleCauseDTO> narrowPossibleCauses(
      @ToolParam(description = "The symptom name or slug") String symptom,
      @ToolParam(description = "Codes of the details the student confirmed, from "
          + "listSymptomFactors", required = false) List<String> confirmedFactorCodes) {

    return slugOf(symptom)
        .map(slug -> possibleCauseService.findPossibleCauses(slug,
            confirmedFactorCodes == null ? List.of() : confirmedFactorCodes))
        .orElseGet(List::of);
  }

  @Tool(description = "Read the current notification, newsletter and usage-tracking settings of "
      + "the student you are talking to.")
  public UserPreferencesDTO myPreferences() {
    // No parameter, and none possible: the service resolves the user from the security context, so
    // there is no argument the model could supply to read somebody else's settings.
    return userPreferencesService.findMyPreferences();
  }

  /**
   * Accepts either a slug the model copied from an earlier tool result or a name a student typed,
   * because it will confidently do both.
   */
  private Optional<String> slugOf(String symptom) {
    if (StringUtils.isBlank(symptom)) {
      return Optional.empty();
    }

    String trimmed = symptom.trim();
    if (trimmed.contains("-") && trimmed.equals(trimmed.toLowerCase(Locale.ROOT))) {
      return Optional.of(trimmed);
    }

    return symptomResolver.resolve(trimmed);
  }
}

package gr.uniwa.unihealth.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The survey answers as they cross the wire.
 *
 * <p>Every answer is a plain {@code String} holding an enum constant name, matching
 * {@link OptionalHealthProfileDTO}. MapStruct converts to and from the enum, so an unrecognised
 * value fails loudly rather than arriving as a silent null — which for a survey would mean quietly
 * discarding somebody's answer.
 *
 * <p>No {@code @NotNull} anywhere: the questions are required in the form, not in the contract. A
 * student who has not answered yet reads this DTO back empty, and the same shape has to serve both
 * directions.
 */
@Getter
@Setter
public class UserSurveyDTO extends BaseUpdatableDTO {

  private String overallUsefulness;

  private String easeOfFinding;

  private String mostUsedSection;

  private String suggestionMatch;

  private String formLength;

  private String aiHelpfulness;

  private String languageBarrier;

  private String dataComfort;

  private String emailFrequency;

  @Size(max = 1000)
  private String improvement;
}

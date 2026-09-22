package gr.uniwa.unihealth.backend.model;

import gr.uniwa.unihealth.backend.model.enums.survey.SurveyComfort;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveyEmailFrequency;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveyFormLength;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveyHelpfulness;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveyLanguageImpact;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveyRating5;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveySection;
import gr.uniwa.unihealth.backend.model.enums.survey.SurveySuggestionMatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One student's answers to the «Έρευνα UniHealth» survey.
 *
 * <p>Extends {@link BaseUpdatableEntity} rather than {@link BaseEntity} because this row is
 * genuinely edited: answering the survey a second time overwrites the first response instead of
 * adding another. That is also why there is no submission timestamp of its own — {@code modifiedOn}
 * already says when the answers were last given.
 */
@Getter
@Setter
@Entity
@Table(name = "t_user_survey")
public class UserSurvey extends BaseUpdatableEntity {

  /**
   * The student who answered.
   *
   * <p>Keyed on the user directly rather than on their health profile, so the survey can be
   * answered by someone who has not completed that form — which is most of the people worth asking.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "overall_usefulness")
  private SurveyRating5 overallUsefulness;

  @Enumerated(EnumType.STRING)
  @Column(name = "ease_of_finding")
  private SurveyRating5 easeOfFinding;

  @Enumerated(EnumType.STRING)
  @Column(name = "most_used_section")
  private SurveySection mostUsedSection;

  @Enumerated(EnumType.STRING)
  @Column(name = "suggestion_match")
  private SurveySuggestionMatch suggestionMatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "form_length")
  private SurveyFormLength formLength;

  @Enumerated(EnumType.STRING)
  @Column(name = "ai_helpfulness")
  private SurveyHelpfulness aiHelpfulness;

  @Enumerated(EnumType.STRING)
  @Column(name = "language_barrier")
  private SurveyLanguageImpact languageBarrier;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_comfort")
  private SurveyComfort dataComfort;

  @Enumerated(EnumType.STRING)
  @Column(name = "email_frequency")
  private SurveyEmailFrequency emailFrequency;

  /** The one free-text answer, and the one that is allowed to be empty. */
  private String improvement;
}

import {SelectOption} from '../components/constant-select/constant-select';
import {BaseUpdateableEntity} from './baseEntity';

/*
 * Hand-mirrored from the Java enums under model/enums/survey/, the same way
 * optional-health-profile.ts mirrors the health-form enums. The wire value is the constant name, and
 * the text a student reads is looked up from the lexicon — never sent.
 *
 * Adding a constant means touching three places: the Java enum, the union below, and the lexicon.
 * The first two are compile errors if forgotten; the third is silent, so it is worth checking.
 */

/** A five-point "how much" scale, shared by the first two questions. */
export type SurveyRating5 = 'NOT_AT_ALL' | 'A_LITTLE' | 'MODERATELY' | 'QUITE' | 'VERY';

export type SurveySection =
    'TOPICS' | 'ADVICE' | 'SYMPTOMS' | 'AI_CHAT' | 'NEWS' | 'MY_TESTS' | 'PROFILE' | 'NONE_YET';

export type SurveySuggestionMatch = 'YES_CLOSELY' | 'MOSTLY' | 'NOT_REALLY' | 'NOT_NOTICED';

export type SurveyFormLength = 'SHORT' | 'ABOUT_RIGHT' | 'LONG' | 'TOO_LONG_UNFINISHED';

export type SurveyHelpfulness = 'VERY' | 'QUITE' | 'A_LITTLE' | 'NOT_AT_ALL' | 'NOT_USED';

export type SurveyLanguageImpact = 'A_LOT' | 'A_LITTLE' | 'NOT_AT_ALL';

export type SurveyComfort = 'VERY' | 'QUITE' | 'A_LITTLE' | 'NOT_AT_ALL';

export type SurveyEmailFrequency = 'NEVER' | 'MONTHLY' | 'WEEKLY' | 'REMINDERS_ONLY';

/**
 * A response. Every answer is optional on the wire even though the form requires it, because the
 * same shape is read back for a student who has not answered yet.
 */
export interface SurveyDTO extends BaseUpdateableEntity {
    overallUsefulness?: SurveyRating5;
    easeOfFinding?: SurveyRating5;
    mostUsedSection?: SurveySection;
    suggestionMatch?: SurveySuggestionMatch;
    formLength?: SurveyFormLength;
    aiHelpfulness?: SurveyHelpfulness;
    languageBarrier?: SurveyLanguageImpact;
    dataComfort?: SurveyComfort;
    emailFrequency?: SurveyEmailFrequency;
    improvement?: string;
}

/**
 * Builds the option list for one question.
 *
 * The lexicon key is derived from the question number and the constant — `survey.q3.option.TOPICS` —
 * so the keys never have to be written out twice. They are therefore invisible to a grep, which is
 * why the verification notes check them by hand.
 */
function optionsFor<K extends string>(
    question: number, values: readonly K[]): readonly SelectOption<K>[] {
    return values.map((key) => ({key, label: `survey.q${question}.option.${key}`}));
}

const RATING_5: readonly SurveyRating5[] =
    ['NOT_AT_ALL', 'A_LITTLE', 'MODERATELY', 'QUITE', 'VERY'];

export const SURVEY_USEFULNESS_OPTIONS = optionsFor(1, RATING_5);
export const SURVEY_EASE_OPTIONS = optionsFor(2, RATING_5);

export const SURVEY_SECTION_OPTIONS = optionsFor<SurveySection>(3,
    ['TOPICS', 'ADVICE', 'SYMPTOMS', 'AI_CHAT', 'NEWS', 'MY_TESTS', 'PROFILE', 'NONE_YET']);

export const SURVEY_SUGGESTION_OPTIONS = optionsFor<SurveySuggestionMatch>(4,
    ['YES_CLOSELY', 'MOSTLY', 'NOT_REALLY', 'NOT_NOTICED']);

export const SURVEY_FORM_LENGTH_OPTIONS = optionsFor<SurveyFormLength>(5,
    ['SHORT', 'ABOUT_RIGHT', 'LONG', 'TOO_LONG_UNFINISHED']);

export const SURVEY_AI_OPTIONS = optionsFor<SurveyHelpfulness>(6,
    ['VERY', 'QUITE', 'A_LITTLE', 'NOT_AT_ALL', 'NOT_USED']);

export const SURVEY_LANGUAGE_OPTIONS = optionsFor<SurveyLanguageImpact>(7,
    ['A_LOT', 'A_LITTLE', 'NOT_AT_ALL']);

export const SURVEY_COMFORT_OPTIONS = optionsFor<SurveyComfort>(8,
    ['VERY', 'QUITE', 'A_LITTLE', 'NOT_AT_ALL']);

export const SURVEY_EMAIL_OPTIONS = optionsFor<SurveyEmailFrequency>(9,
    ['NEVER', 'MONTHLY', 'WEEKLY', 'REMINDERS_ONLY']);

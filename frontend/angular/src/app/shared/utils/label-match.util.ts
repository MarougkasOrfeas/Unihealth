import {LabelMatchResult, LabelTargeted} from '../interfaces/label-targeted';
import {UserProfileLabel} from '../interfaces/user-profile-label';

/** Added once per stated preference the content targets. */
export const PREFERENCE_BOOST_POINTS = 100;
/** Subtracted once per safety concern the content targets. Outweighs a boost on purpose. */
export const SAFETY_PENALTY_POINTS = 150;

const PROFILE_LABEL_KEY_PREFIX = 'profile.label.';

/** Lexicon key holding the second-person wording for a label code, e.g. "you want to sleep better". */
export const profileLabelKey = (code: string): string => PROFILE_LABEL_KEY_PREFIX + code;

/**
 * Turns the labels returned by `GET /profile/_me/labels` into a lookup of code to priority. Built
 * once per render rather than per scored item, which is why it is separate from the scorer.
 */
export function labelPriorityMap(labels: readonly UserProfileLabel[]): Map<string, number> {
    return new Map(labels.map(label => [label.code, label.priority]));
}

/**
 * Scores one piece of content against one user's labels.
 *
 * The weighting is the user's own priority for a label times the content's weight for it, so a
 * high-priority signal such as a chronic condition dominates a low-priority one such as gender.
 */
export function scoreAgainstLabels(
    item: LabelTargeted,
    priorities: ReadonlyMap<string, number>,
): LabelMatchResult {
    const matchedLabels = item.matchedLabels
        .filter(code => priorities.has(code))
        .sort((a, b) => (priorities.get(b) ?? 0) - (priorities.get(a) ?? 0));

    const weightedMatchScore = matchedLabels.reduce((score, code) => {
        const priority = priorities.get(code) ?? 0;
        const weight = item.labelWeights[code] ?? 1;
        return score + priority * weight;
    }, 0);

    const preferenceBoost = (item.preferenceBoostLabels ?? [])
        .filter(code => priorities.has(code)).length * PREFERENCE_BOOST_POINTS;

    const safetyPenalty = (item.safetyPenaltyLabels ?? [])
        .filter(code => priorities.has(code)).length * SAFETY_PENALTY_POINTS;

    return {
        score: Math.max(0, Math.round(weightedMatchScore + preferenceBoost - safetyPenalty)),
        matchedLabels,
    };
}

/**
 * Last-resort readable text for a label code that has no lexicon entry yet. Label codes are
 * calculated on the backend and new ones can appear before anyone writes a translation for them,
 * so this keeps a recommendation's reason readable instead of leaking `OPTIONAL_EXERCISE_NONE`.
 */
export function humaniseLabelCode(code: string): string {
    return code.replace(/^(OPTIONAL|GOAL|HAS)_/, '').replace(/_/g, ' ').toLowerCase();
}

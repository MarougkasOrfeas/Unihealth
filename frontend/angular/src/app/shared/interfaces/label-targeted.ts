/**
 * Implemented by any piece of content that can be aimed at a user's profiling labels — health
 * topics today, advice sections next. Keeping it here rather than next to either feature is what
 * lets both pages share one scorer (see `shared/utils/label-match.util.ts`).
 *
 * The codes are the raw `UPPER_SNAKE_CASE` label codes the backend calculates and returns from
 * `GET /profile/_me/labels`.
 */
export interface LabelTargeted {
    /** Label codes this content is aimed at. */
    matchedLabels: string[];
    /** Per-label multiplier applied to the user's own priority. Missing entries count as 1. */
    labelWeights: Record<string, number>;
    /** Codes that make this content a notably better fit, e.g. a stated goal. */
    preferenceBoostLabels?: string[];
    /** Codes that make this content a worse or unsafe fit, e.g. a contraindication. */
    safetyPenaltyLabels?: string[];
}

/** The outcome of scoring one {@link LabelTargeted} item against one user's labels. */
export interface LabelMatchResult {
    /** Never negative — a penalised item drops to 0 rather than below the unmatched ones. */
    score: number;
    /**
     * Only the codes the user actually has, sorted by the user's own priority descending, so the
     * first few are the most meaningful things to show them as the reason for a recommendation.
     */
    matchedLabels: string[];
}

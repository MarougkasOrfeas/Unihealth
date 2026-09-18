export interface UserProfileLabel {
    code: string;
    priority: number;
    /**
     * The family this label belongs to, e.g. 'bmi', 'optional_sleep', 'optional_nutrition'.
     *
     * Optional because a label with no active mapping has no group — the same case the backend
     * falls back to a calculated priority for.
     */
    labelGroup?: string | null;
}

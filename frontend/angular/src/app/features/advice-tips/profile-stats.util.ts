import {HealthProfileViewDTO} from '../../shared/interfaces/health-profile';
import {OptionalHealthProfile} from '../../shared/interfaces/optional-health-profile';
import {UserProfileLabel} from '../../shared/interfaces/user-profile-label';

/**
 * Statistics for the advice page, derived entirely from what the user actually told us.
 *
 * Nothing here is invented or benchmarked against other users: every number traces back to
 * `GET /profile/_me`, `GET /profile/_me/optional` or `GET /profile/_me/labels`. There is
 * deliberately no composite "health score" — averaging unrelated lifestyle answers into a single
 * figure would look precise while meaning nothing.
 */

/** How the user's own answer sits against published general guidance. */
export type GuidanceStatus = 'below' | 'within' | 'above';

export interface LifestyleMetric {
    /** Field name on the optional profile, used as the track id. */
    key: string;
    /** Lexicon key for the metric name, e.g. "Sleep". */
    labelKey: string;
    /** Lexicon key for the user's own answer — reuses the optional-form option wording. */
    valueKey: string;
    /** Lexicon key for the general guidance this is compared against. */
    guidanceKey: string;
    status: GuidanceStatus;
    /**
     * 0-100, used only for the length of the bar. It expresses how close the answer sits to
     * guidance, not a score out of 100, and is never summed or averaged across metrics.
     */
    position: number;
}

export interface FocusArea {
    /** Raw `labelGroup` from the backend. */
    group: string;
    /** Lexicon key for the group name. */
    labelKey: string;
    /** Summed priority of the user's labels in this group. */
    weight: number;
    /** How many of the user's labels fall in this group. */
    count: number;
}

export interface ProfileSnapshot {
    bmi: string | null;
    /** Lexicon key for the BMI band, or null when height/weight are not filled in. */
    bmiBandKey: string | null;
    /** Percentage of the optional form that has been answered. */
    completeness: number;
    answeredCount: number;
    totalCount: number;
    /** How many profiling labels the user currently has. */
    signalCount: number;
    /** Of those, how many are safety-critical and therefore ranked first. */
    safetySignalCount: number;
}

interface GuidanceEntry {
    status: GuidanceStatus;
    position: number;
}

/**
 * Where each answer sits against general public guidance — roughly 7-9 hours of sleep, 6-8 glasses
 * of water, 3-5 exercise sessions a week, limited screen time and caffeine, no smoking.
 *
 * These are coarse bands for orientation, not clinical thresholds, which is why the UI presents
 * them as guidance rather than as a verdict.
 */
const GUIDANCE: Record<string, Record<string, GuidanceEntry>> = {
    sleepQuality: {
        LESS_THAN_6_HOURS: {status: 'below', position: 20},
        ABOUT_6_HOURS: {status: 'below', position: 45},
        ABOUT_8_HOURS: {status: 'within', position: 90},
        MORE_THAN_8_HOURS: {status: 'within', position: 80},
    },
    water: {
        ONE_TO_THREE_GLASSES: {status: 'below', position: 25},
        FOUR_TO_SIX_GLASSES: {status: 'below', position: 55},
        SEVEN_TO_NINE_GLASSES: {status: 'within', position: 90},
        MORE_THAN_NINE_GLASSES: {status: 'within', position: 85},
    },
    exercise: {
        NONE: {status: 'below', position: 10},
        ONE_TO_TWO_TIMES: {status: 'below', position: 40},
        THREE_TO_FIVE_TIMES: {status: 'within', position: 90},
        FIVE_TO_SEVEN_TIMES: {status: 'within', position: 85},
    },
    screenTime: {
        LESS_THAN_2_HOURS: {status: 'within', position: 95},
        TWO_TO_FOUR_HOURS: {status: 'within', position: 75},
        FOUR_TO_SIX_HOURS: {status: 'above', position: 45},
        MORE_THAN_6_HOURS: {status: 'above', position: 20},
    },
    coffee: {
        NONE: {status: 'within', position: 95},
        ONE_CUP: {status: 'within', position: 90},
        TWO_TO_THREE_CUPS: {status: 'within', position: 65},
        FOUR_OR_MORE_CUPS: {status: 'above', position: 30},
    },
    smoking: {
        NO: {status: 'within', position: 100},
        OCCASIONALLY: {status: 'above', position: 40},
        DAILY: {status: 'above', position: 10},
    },
};

/** Order the metrics appear in. Also the set that has guidance to compare against. */
const METRIC_KEYS = ['sleepQuality', 'water', 'exercise', 'screenTime', 'coffee', 'smoking'] as const;
type MetricKey = typeof METRIC_KEYS[number];

/**
 * Fields counted towards completeness. Conditional detail fields and free text are excluded, the
 * same way `OptionalFormReminderServiceImpl` does on the backend, so a user who legitimately has
 * nothing to add is not treated as having an incomplete profile.
 */
const COMPLETENESS_FIELDS: (keyof OptionalHealthProfile)[] = [
    'sleepQuality', 'studyLoad', 'smoking', 'coffee', 'screenTime', 'exercise',
    'mealsPerDay', 'eatSnack', 'water', 'dietType', 'medication', 'surgeryHistory',
    'preferredContent', 'frequency',
];

/** Labels that must rank above lifestyle signals. Mirrors the backend's own ordering. */
const SAFETY_PREFIXES = ['CHRONIC_', 'ALLERGY_', 'HAS_CHRONIC', 'HAS_FOOD_ALLERGY', 'OPTIONAL_HIGH_'];

/** The user's answers compared with general guidance. Unanswered fields are simply omitted. */
export function buildLifestyleMetrics(
    optional: OptionalHealthProfile | null,
): LifestyleMetric[] {
    if (!optional) {
        return [];
    }

    const metrics: LifestyleMetric[] = [];

    for (const key of METRIC_KEYS) {
        const answer = optional[key as MetricKey] as string | null;
        if (!answer) {
            continue;
        }

        const guidance = GUIDANCE[key]?.[answer];
        if (!guidance) {
            continue;
        }

        metrics.push({
            key,
            labelKey: `advice.metric.${key}`,
            valueKey: `complete.health.optional.${key}.options.${answer}`,
            guidanceKey: `advice.metric.${key}.guidance`,
            status: guidance.status,
            position: guidance.position,
        });
    }

    return metrics;
}

/**
 * Where the user's profiling signals concentrate, by summed priority.
 *
 * Grouping uses the backend's own `labelGroup` rather than guessing families from the shape of the
 * code. Labels without a group are skipped instead of being lumped into a misleading "other".
 */
export function buildFocusAreas(labels: readonly UserProfileLabel[]): FocusArea[] {
    const totals = new Map<string, {weight: number; count: number}>();

    for (const label of labels) {
        const group = label.labelGroup;
        if (!group) {
            continue;
        }

        const current = totals.get(group) ?? {weight: 0, count: 0};
        current.weight += label.priority;
        current.count += 1;
        totals.set(group, current);
    }

    return [...totals.entries()]
        .map(([group, {weight, count}]) => ({
            group,
            labelKey: `advice.labelGroup.${group}`,
            weight,
            count,
        }))
        .sort((a, b) => b.weight - a.weight);
}

export function buildSnapshot(
    profile: HealthProfileViewDTO | null,
    optional: OptionalHealthProfile | null,
    labels: readonly UserProfileLabel[],
): ProfileSnapshot {
    const answeredCount = optional
        ? COMPLETENESS_FIELDS.filter(field => {
            const value = optional[field];
            return value !== null && value !== undefined && value !== '';
        }).length
        : 0;

    return {
        bmi: profile?.bmi ?? null,
        bmiBandKey: bmiBandKey(profile?.bmi ?? null),
        completeness: Math.round((answeredCount / COMPLETENESS_FIELDS.length) * 100),
        answeredCount,
        totalCount: COMPLETENESS_FIELDS.length,
        signalCount: labels.length,
        safetySignalCount: labels.filter(label =>
            SAFETY_PREFIXES.some(prefix => label.code.startsWith(prefix))).length,
    };
}

/** WHO adult BMI bands. Returns null for a missing or unparseable value rather than guessing. */
export function bmiBandKey(bmi: string | null): string | null {
    if (!bmi) {
        return null;
    }

    const value = Number.parseFloat(bmi);
    if (!Number.isFinite(value) || value <= 0) {
        return null;
    }

    if (value < 18.5) {
        return 'advice.bmi.band.UNDERWEIGHT';
    }
    if (value < 25) {
        return 'advice.bmi.band.NORMAL';
    }
    if (value < 30) {
        return 'advice.bmi.band.OVERWEIGHT';
    }
    if (value < 35) {
        return 'advice.bmi.band.OBESE';
    }
    return 'advice.bmi.band.SEVERELY_OBESE';
}

// --- profile need vs. measured engagement ------------------------------------

/**
 * Groups that content can actually be tagged against, and therefore the only ones where a
 * need-versus-engagement comparison means anything.
 *
 * Demographics (`gender`, `age`, `height`, `weight`) and content-format preferences
 * (`optional_preference`) are excluded deliberately: no topic or advice section targets them, so
 * their engagement share could only ever be 0% — a large "need" bar against an empty "read" bar,
 * which reads as a failing when it is really just untargetable.
 */
const COMPARABLE_GROUPS: ReadonlySet<string> = new Set([
    'optional_sleep', 'optional_nutrition', 'optional_activity', 'optional_substance',
    'optional_context', 'optional_risk', 'goal', 'bmi', 'allergy', 'mixed',
]);

/** Rows beyond this add noise rather than insight; the tail is counted, not drawn. */
export const ATTENTION_ROW_COUNT = 6;

/** One measurement of engagement with a single label, as returned by `GET /usage/_me`. */
export interface LabelUsageTotals {
    labelCode: string;
    viewSeconds: number;
    interactionCount: number;
}

export interface AttentionRow {
    group: string;
    /** Reuses the existing `advice.labelGroup.<group>` wording. */
    labelKey: string;
    /** Share of summed label priority across comparable groups, 0-100. */
    needShare: number;
    /** Share of summed view seconds across comparable groups, 0-100. */
    readShare: number;
    /** `readShare - needShare`, in percentage points. Positive means read more than indicated. */
    deltaPoints: number;
    /** Raw figures, for the hover title only — never for the bar length. */
    needWeight: number;
    readSeconds: number;
}

export interface AttentionComparison {
    rows: AttentionRow[];
    /** Comparable groups that exist but fell outside {@link ATTENTION_ROW_COUNT}. */
    hiddenCount: number;
    /** False when no seconds were recorded, which renders the need side alone. */
    hasEngagement: boolean;
}

/**
 * Compares what the profile indicates against what the user actually reads.
 *
 * The two sides measure genuinely different things — priority is a significance score, seconds are
 * attention — so each is normalised against **its own** total. That is what makes a single shared
 * scale legitimate: neither axis is being borrowed for the other's units.
 *
 * The code-to-group join uses the labels the caller already fetched rather than a new endpoint.
 * That is sound because the backend only ever stores usage for labels the user owns, so every row
 * that matters has a label to join against.
 */
export function buildAttentionComparison(
    labels: readonly UserProfileLabel[],
    usage: readonly LabelUsageTotals[],
): AttentionComparison {
    const groupOf = new Map<string, string>();
    for (const label of labels) {
        // A deactivated mapping yields a null group. Dropped from both sides, exactly as
        // `buildFocusAreas` already drops it, so the two denominators stay consistent.
        if (label.labelGroup) {
            groupOf.set(label.code, label.labelGroup);
        }
    }

    const needByGroup = new Map<string, number>();
    for (const label of labels) {
        const group = label.labelGroup;
        if (!group || !COMPARABLE_GROUPS.has(group)) {
            continue;
        }
        needByGroup.set(group, (needByGroup.get(group) ?? 0) + label.priority);
    }

    const readByGroup = new Map<string, number>();
    for (const entry of usage) {
        // Usage for a label the user has since lost (a profile re-save clears the optional ones)
        // has nothing to join to, so it is dropped: there is no current need to compare it against.
        const group = groupOf.get(entry.labelCode);
        if (!group || !COMPARABLE_GROUPS.has(group)) {
            continue;
        }
        readByGroup.set(group, (readByGroup.get(group) ?? 0) + entry.viewSeconds);
    }

    const needTotal = sum(needByGroup.values());
    const readTotal = sum(readByGroup.values());

    if (needTotal === 0) {
        return {rows: [], hiddenCount: 0, hasEngagement: false};
    }

    const all: AttentionRow[] = [...new Set([...needByGroup.keys(), ...readByGroup.keys()])]
        .map((group) => {
            const needWeight = needByGroup.get(group) ?? 0;
            const readSeconds = readByGroup.get(group) ?? 0;
            // Rounded independently, so a column can total 99 or 101. Nobody adds these up, and
            // largest-remainder correction would be precision theatre on a bar chart.
            const needShare = Math.round((needWeight / needTotal) * 100);
            const readShare = readTotal > 0 ? Math.round((readSeconds / readTotal) * 100) : 0;

            return {
                group,
                labelKey: `advice.labelGroup.${group}`,
                needShare,
                readShare,
                deltaPoints: readShare - needShare,
                needWeight,
                readSeconds,
            };
        })
        .filter((row) => row.needWeight > 0 || row.readSeconds > 0)
        // Need descending: a stable reference order, so rows do not reshuffle as seconds accrue.
        .sort((a, b) => b.needShare - a.needShare || b.readShare - a.readShare);

    return {
        rows: all.slice(0, ATTENTION_ROW_COUNT),
        hiddenCount: Math.max(0, all.length - ATTENTION_ROW_COUNT),
        hasEngagement: readTotal > 0,
    };
}

function sum(values: Iterable<number>): number {
    let total = 0;
    for (const value of values) {
        total += value;
    }
    return total;
}

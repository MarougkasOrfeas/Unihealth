import {LabelTargeted} from '../../shared/interfaces/label-targeted';

/**
 * Stored as a code and rendered through `topics.category.<CODE>`, the same way
 * `UnihealthAssistantItem.section` works, so the chip is translatable and the value survives a
 * move into a database column unchanged.
 */
export type HealthTopicCategory =
    | 'PREVENTIVE_CARE'
    | 'NUTRITION'
    | 'MENTAL_HEALTH'
    | 'FITNESS'
    | 'STUDENT_HEALTH';

export const HEALTH_TOPIC_CATEGORIES: readonly HealthTopicCategory[] = [
    'PREVENTIVE_CARE',
    'NUTRITION',
    'MENTAL_HEALTH',
    'FITNESS',
    'STUDENT_HEALTH',
];

/** Pseudo-category for the "show everything" filter button. Never stored on a topic. */
export const ALL_CATEGORIES = 'All' as const;
export type HealthTopicCategoryFilter = HealthTopicCategory | typeof ALL_CATEGORIES;

/**
 * One health topic. The field names deliberately mirror `UnihealthAssistantItem` so that when real
 * content is ingested into a `t_health_topic` table, the DTO is a transcription of this interface
 * rather than a translation of it.
 *
 * Only `HealthTopicsService` is allowed to know where instances come from — nothing else may
 * import the mock, which is what keeps the ingestion swap to a single file.
 */
export interface HealthTopic extends LabelTargeted {
    id: string;
    title: string;
    /** The short text shown on the card. Mirrors `UnihealthAssistantItem.brief`. */
    brief: string;
    /** The full text shown in the modal. Mirrors `UnihealthAssistantItem.content`. */
    content: string;
    category: HealthTopicCategory;
    tags: string[];
    /** Fallback ordering when no personalisation applies. */
    displayOrder: number;
    active: boolean;

    icon: string;
    accentColor: string;

    // Optional because ingested content may not carry editorial metadata; every template that
    // renders these must guard them.
    readTime?: string;
    updated?: string;
    reviewedBy?: string;
    keyActions?: string[];
    warningSigns?: string[];

    /** 1 = most trending; absent = not trending at all. Backend-computable from views later. */
    trendingRank?: number;
}

/**
 * A topic decorated with everything that depends on *this* user. Never persisted, which is why
 * `isFavourite` lives here and not on {@link HealthTopic}.
 */
export interface HealthTopicView extends HealthTopic {
    /** Relevance against the user's profiling labels; 0 when nothing matched. */
    score: number;
    /**
     * The codes the *user* has that this topic targets, ordered by the user's own priority.
     *
     * Deliberately not called `matchedLabels`: that name already belongs to the topic's targeting
     * list inherited from `LabelTargeted`, and spreading a match result over a topic would
     * silently overwrite it.
     */
    matchedUserLabels: string[];
    isFavourite: boolean;
}

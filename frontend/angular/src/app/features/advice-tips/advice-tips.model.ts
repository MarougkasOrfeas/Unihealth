import {LabelTargeted} from '../../shared/interfaces/label-targeted';

/**
 * One actionable tip. Field names mirror `HealthTopic` so both content types can be served by the
 * same shape of backend table when real content is ingested.
 */
export interface AdviceTip {
    id: string;
    title: string;
    /** The short text on the card. */
    brief: string;
    /** The full text in the modal. */
    content: string;
}

/** A themed group of tips, aimed at a set of profiling labels. */
export interface AdviceSection extends LabelTargeted {
    id: string;
    title: string;
    subtitle: string;
    icon: string;
    accentColor: string;
    displayOrder: number;
    active: boolean;
    tips: AdviceTip[];
}

/** A section scored against the current user. Never persisted. */
export interface AdviceSectionView extends AdviceSection {
    score: number;
    /** Codes the *user* has that this section targets, ordered by their own priority. */
    matchedUserLabels: string[];
}

/** A tip lifted out of its section for the suggestion cards, carrying its section's identity. */
export interface AdviceTipView extends AdviceTip {
    icon: string;
    accentColor: string;
    sectionId: string;
    sectionTitle: string;
    matchedUserLabels: string[];
}

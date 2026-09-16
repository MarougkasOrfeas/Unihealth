import {BaseEntity} from "./baseEntity";

export interface SymptomItem extends BaseEntity {
    title: string;
    slug: string;
    startingLetter: string;
    brief?: string;
    synonyms?: string;
    /** Whether this symptom has an advanced search to offer. */
    hasFactors: boolean;
    displayOrder: number;
    active: boolean;
}

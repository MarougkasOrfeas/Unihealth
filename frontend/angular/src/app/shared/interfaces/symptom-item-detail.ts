import {BaseEntity} from "./baseEntity";
import {DataSource} from "./data-source";

export interface SymptomItemDetail extends BaseEntity {
    title: string;
    slug: string;
    startingLetter: string;
    brief?: string;
    synonyms?: string;
    overviewText?: string;
    symptomsText?: string;
    doText?: string;
    dontText?: string;
    seeDoctorIfText?: string;
    /** Get help today. Shown above the article, never collapsed. */
    urgentText?: string;
    /** Call 999. Shown above the article, never collapsed. */
    emergencyText?: string;
    treatmentText?: string;
    causesText?: string;
    hasFactors: boolean;
    source?: DataSource;
    sourceUrl?: string;
    sourceLastReviewed?: string;
    active: boolean;
}

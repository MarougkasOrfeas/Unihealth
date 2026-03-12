import {BaseEntity} from "./baseEntity";

export interface SymptomItemDetail extends BaseEntity {
    title: string;
    slug: string;
    startingLetter: string;
    brief?: string;
    overviewText?: string;
    symptomsText?: string;
    doText?: string;
    dontText?: string;
    seeDoctorIfText?: string;
    treatmentText?: string;
    causesText?: string;
    active: boolean;
}

import {BaseEntity} from "./baseEntity";

/** One detail of a symptom that a reader can tick to narrow it down. */
export interface SymptomFactor extends BaseEntity {
    code: string;
    label: string;
    displayOrder: number;
}

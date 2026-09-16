import {BaseEntity} from "./baseEntity";
import {DataSource} from "./data-source";
import {SymptomItem} from "./symptom-item";

export interface ConditionDetail extends BaseEntity {
    name: string;
    slug: string;
    startingLetter: string;
    sourceUrl?: string;
    source?: DataSource;
    relatedSymptoms: SymptomItem[];
    active: boolean;
}

import {BaseEntity} from "./baseEntity";

export interface ConditionItem extends BaseEntity {
    name: string;
    slug: string;
    startingLetter: string;
    sourceUrl?: string;
    displayOrder: number;
    active: boolean;
}

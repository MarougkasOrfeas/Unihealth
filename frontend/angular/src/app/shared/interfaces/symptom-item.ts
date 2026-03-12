import {BaseEntity} from "./baseEntity";

export interface SymptomItem extends BaseEntity {
    title: string;
    slug: string;
    startingLetter: string;
    brief?: string;
    displayOrder: number;
    active: boolean;
}

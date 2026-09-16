import {BaseEntity} from "./baseEntity";

/** An external corpus the content was ingested from, with the licence it is reused under. */
export interface DataSource extends BaseEntity {
    code: string;
    name: string;
    url: string;
    licence: string;
    licenceUrl?: string;
    attributionText?: string;
    logoUrl?: string;
    retrievedOn?: string;
    displayOrder: number;
    active: boolean;
}

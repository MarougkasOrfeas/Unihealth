import {BaseEntity} from "./baseEntity";

export interface GroupDTO extends BaseEntity {
    name: string;
    description: string;
    departments: string[];
}

export interface UniGroupOption {
    id?: string;
    name: string;
    description?: string | null;
    active?: boolean;
}

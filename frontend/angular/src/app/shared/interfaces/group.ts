import {BaseEntity} from "./baseEntity";

export interface GroupDTO extends BaseEntity {
    name: string;
    description: string;
    active: boolean;
    /** Department names. Only populated by the paged list endpoint, not by `GET /group/{id}`. */
    departments: string[];
}

export interface UniGroupOption {
    id?: string;
    name: string;
    description?: string | null;
    active?: boolean;
}

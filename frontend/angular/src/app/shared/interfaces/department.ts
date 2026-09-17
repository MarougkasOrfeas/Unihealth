import {BaseEntity} from "./baseEntity";

export interface DepartmentDTO extends BaseEntity {
    name: string;
    description: string;
    active: boolean;
    /** Name of the school this department belongs to. Required on create and update. */
    group: string;
    /** Usernames. Only populated by the paged list endpoint, not by `GET /department/{id}`. */
    users: string[];
}

export interface DepartmentOption {
    id?: string;
    name: string;
    description?: string | null;
    active?: boolean;
    groupName?: string | null;
}


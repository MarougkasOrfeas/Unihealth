import {BaseEntity} from "./baseEntity";

export interface DepartmentDTO extends BaseEntity {
    name: string;
    description: string;
    users: string[];
}

export interface DepartmentOption {
    id?: string;
    name: string;
    description?: string | null;
    active?: boolean;
    groupName?: string | null;
}


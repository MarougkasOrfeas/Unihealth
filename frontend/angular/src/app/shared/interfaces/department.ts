import {BaseEntity} from "./baseEntity";

export interface DepartmentDTO extends BaseEntity {
    name: string;
    description: string;
    users: string[];
}

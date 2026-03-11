import {BaseEntity} from "./baseEntity";

export interface GroupDTO extends BaseEntity {
    name: string;
    description: string;
    departments: string[];
}

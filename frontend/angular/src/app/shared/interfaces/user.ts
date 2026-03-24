import {BaseEntity} from "./baseEntity";

export interface User extends BaseEntity {
    username: string;
    email: string;
    firstname: string;
    lastname: string;
    phoneNumber?: string | null;
    deactivateAfter?: string | null;
    deactivationMode?: string | null;
    language: string;
    lastLogin: string;
    status: string;
    role: string;
    group: string;
    department: string;
    deactivatedDueToInactivity: boolean;
    scheduledDeactivationReason?: string | null;
    deactivationReason?: string | null;
    reactivationReason?: string | null;
}

export enum UserStatus {
    DEACTIVATED,
    ACTIVE,
    UNVERIFIED
}

export interface RightsMatrix {
    userId: string;
    globalPermissions: string[];
}

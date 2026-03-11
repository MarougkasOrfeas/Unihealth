export interface User {
    id?: string;
    username: string;
    email: string;
    firstname: string;
    lastname: string;
    language: string;
    lastLogin: string;
    status: string;
    role: string;
    department: string;
}

export enum UserStatus {
    DEACTIVATED,
    ACTIVE,
    UNVERIFIED
}

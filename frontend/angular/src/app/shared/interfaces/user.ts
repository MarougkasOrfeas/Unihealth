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
    /** `BaseService.transform` converts the ISO string into a `Date` before the caller sees it. */
    lastLogin: Date | string | null;
    status: string;
    role: string;
    group: string;
    department: string;
    deactivatedDueToInactivity: boolean;
    scheduledDeactivationReason?: string | null;
    deactivationReason?: string | null;
    reactivationReason?: string | null;
}

/**
 * String values on purpose: the backend enum is serialised by name (`"ACTIVE"`), and `UserDTO`
 * returns it that way. A numeric enum only worked because Jackson falls back to ordinals and the
 * two declaration orders happened to match — reordering either side would have broken it silently.
 */
export enum UserStatus {
    DEACTIVATED = 'DEACTIVATED',
    ACTIVE = 'ACTIVE',
    UNVERIFIED = 'UNVERIFIED',
}

export interface RightsMatrix {
    userId: string;
    globalPermissions: string[];
}

/**
 * The email preferences a user manages for themselves under Profile > Preferences.
 *
 * Stored positive: the profile screen renders `newsletterSubscribed` inverted as
 * "Unsubscribe from News Feeds", so the persisted value never has to be read as a double negative.
 */
export interface UserPreferences {
    newsletterSubscribed: boolean;
    notificationsEnabled: boolean;
}

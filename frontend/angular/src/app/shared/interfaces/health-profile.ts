import {BaseEntity} from "./baseEntity";

export interface HealthProfileDTO extends BaseEntity {
    dateOfBirth?: Date | string | null;
    gender?: Gender | null;
    heightCm?: number | null;
    weightKg?: number | null;
    hasFoodAllergies: boolean;
    foodAllergiesDetails?: string | null;
    hasChronicConditions: boolean;
    chronicConditionsDetails?: string | null;
    primaryGoal?: PrimaryGoal | null;
}

export interface HealthProfileViewDTO extends HealthProfileDTO {
    age: number;
    bmi: string;
}

export enum Gender {
    MALE = 'MALE',
    FEMALE = 'FEMALE',
    OTHER = 'OTHER'
}

export enum PrimaryGoal {
    EAT_HEALTHIER = "EAT_HEALTHIER",
    IMPROVE_FITNESS = "IMPROVE_FITNESS",
    MANAGE_WEIGHT = "MANAGE_WEIGHT",
    SLEEP_BETTER = "SLEEP_BETTER",
}

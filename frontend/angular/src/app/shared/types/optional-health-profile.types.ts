export type SleepQuality = 'POOR' | 'AVERAGE' | 'GOOD';
export type StressLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type StudyLoad = 'LOW' | 'MODERATE' | 'HIGH';

export type ActivityLevel = 'LOW' | 'MODERATE' | 'HIGH';
export type DietType = 'OMNIVORE' | 'VEGETARIAN' | 'VEGAN' | 'PESCATARIAN' | 'OTHER';
export type MealRegularity = 'REGULAR' | 'SOMEWHAT_IRREGULAR' | 'IRREGULAR';
export type HydrationLevel = 'LOW' | 'ADEQUATE' | 'HIGH';

export type FitnessLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export type PreferredRoutineTime = 'MORNING' | 'AFTERNOON' | 'EVENING' | 'NO_PREFERENCE';
export type PreferredContentType = 'ARTICLES' | 'TIPS' | 'EXERCISES' | 'MIXED';
export type WellnessFocus = 'SLEEP' | 'STRESS' | 'NUTRITION' | 'FITNESS' | 'GENERAL_WELLBEING';

export const SLEEP_QUALITY_OPTIONS: readonly SleepQuality[] = ['POOR', 'AVERAGE', 'GOOD'];
export const STRESS_LEVEL_OPTIONS: readonly StressLevel[] = ['LOW', 'MEDIUM', 'HIGH'];
export const STUDY_LOAD_OPTIONS: readonly StudyLoad[] = ['LOW', 'MODERATE', 'HIGH'];

export const ACTIVITY_LEVEL_OPTIONS: readonly ActivityLevel[] = ['LOW', 'MODERATE', 'HIGH'];
export const DIET_TYPE_OPTIONS: readonly DietType[] = ['OMNIVORE', 'VEGETARIAN', 'VEGAN', 'PESCATARIAN', 'OTHER'];
export const MEAL_REGULARITY_OPTIONS: readonly MealRegularity[] = ['REGULAR', 'SOMEWHAT_IRREGULAR', 'IRREGULAR'];
export const HYDRATION_LEVEL_OPTIONS: readonly HydrationLevel[] = ['LOW', 'ADEQUATE', 'HIGH'];

export const FITNESS_LEVEL_OPTIONS: readonly FitnessLevel[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

export const PREFERRED_ROUTINE_TIME_OPTIONS: readonly PreferredRoutineTime[] = [
    'MORNING',
    'AFTERNOON',
    'EVENING',
    'NO_PREFERENCE'
];

export const PREFERRED_CONTENT_TYPE_OPTIONS: readonly PreferredContentType[] = [
    'ARTICLES',
    'TIPS',
    'EXERCISES',
    'MIXED'
];

export const WELLNESS_FOCUS_OPTIONS: readonly WellnessFocus[] = [
    'SLEEP',
    'STRESS',
    'NUTRITION',
    'FITNESS',
    'GENERAL_WELLBEING'
];

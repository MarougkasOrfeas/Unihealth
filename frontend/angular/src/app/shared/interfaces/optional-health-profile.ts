import {
    ActivityLevel, DietType, FitnessLevel, HydrationLevel, MealRegularity, PreferredContentType, PreferredRoutineTime,
    SleepQuality,
    StressLevel,
    StudyLoad, WellnessFocus
} from "../types/optional-health-profile.types";


export interface OptionalHealthProfileDto {
    sleepQuality: SleepQuality | null;
    stressLevel: StressLevel | null;
    studyLoad: StudyLoad | null;

    activityLevel: ActivityLevel | null;
    exerciseFrequencyPerWeek: number | null;
    dietType: DietType | null;
    mealRegularity: MealRegularity | null;
    hydrationLevel: HydrationLevel | null;

    fitnessLevel: FitnessLevel | null;
    hasPhysicalLimitations: boolean | null;
    physicalLimitationsDetails: string | null;

    preferredRoutineTime: PreferredRoutineTime | null;
    preferredContentType: PreferredContentType | null;
    wellnessFocus: WellnessFocus | null;
}

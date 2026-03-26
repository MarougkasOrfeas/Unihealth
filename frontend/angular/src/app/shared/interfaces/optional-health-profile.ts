export interface OptionalHealthProfile {
    sleepQuality: SleepQuality | null;
    studyLoad: StudyLoad | null;
    smoking: SmokingHabit | null;
    coffee: CoffeeConsumption | null;
    screenTime: ScreenTime | null;
    exercise: ExerciseFrequency | null;
    mealsPerDay: MealsPerDay | null;
    eatSnack: SnackFrequency | null;
    water: WaterIntake | null;
    dietType: DietType | null;
    medication: boolean | null;
    medicationDetails: string | null;
    surgeryHistory: boolean | null;
    surgeryDetails: string | null;
    preferredContent: PreferredContentType | null;
    frequency: ContentFrequency | null;
    comments: string | null;
}

export type OptionalHealthProfileDto = OptionalHealthProfile;

export enum SleepQuality {
    LESS_THAN_6_HOURS = 'LESS_THAN_6_HOURS',
    ABOUT_6_HOURS = 'ABOUT_6_HOURS',
    ABOUT_8_HOURS = 'ABOUT_8_HOURS',
    MORE_THAN_8_HOURS = 'MORE_THAN_8_HOURS'
}

export enum StudyLoad {
    LESS_THAN_1_HOUR = 'LESS_THAN_1_HOUR',
    ONE_TO_TWO_HOURS = 'ONE_TO_TWO_HOURS',
    TWO_TO_FOUR_HOURS = 'TWO_TO_FOUR_HOURS',
    MORE_THAN_4_HOURS = 'MORE_THAN_4_HOURS'
}

export enum SmokingHabit {
    NO = 'NO',
    OCCASIONALLY = 'OCCASIONALLY',
    DAILY = 'DAILY'
}

export enum CoffeeConsumption {
    NONE = 'NONE',
    ONE_CUP = 'ONE_CUP',
    TWO_TO_THREE_CUPS = 'TWO_TO_THREE_CUPS',
    FOUR_OR_MORE_CUPS = 'FOUR_OR_MORE_CUPS'
}

export enum ScreenTime {
    LESS_THAN_2_HOURS = 'LESS_THAN_2_HOURS',
    TWO_TO_FOUR_HOURS = 'TWO_TO_FOUR_HOURS',
    FOUR_TO_SIX_HOURS = 'FOUR_TO_SIX_HOURS',
    MORE_THAN_6_HOURS = 'MORE_THAN_6_HOURS'
}

export enum ExerciseFrequency {
    NONE = 'NONE',
    ONE_TO_TWO_TIMES = 'ONE_TO_TWO_TIMES',
    THREE_TO_FIVE_TIMES = 'THREE_TO_FIVE_TIMES',
    FIVE_TO_SEVEN_TIMES = 'FIVE_TO_SEVEN_TIMES'
}

export enum MealsPerDay {
    ONE_TO_TWO = 'ONE_TO_TWO',
    THREE = 'THREE',
    FOUR_TO_FIVE = 'FOUR_TO_FIVE',
    MORE_THAN_FIVE = 'MORE_THAN_FIVE'
}

export enum SnackFrequency {
    NO = 'NO',
    SOMETIMES = 'SOMETIMES',
    DAILY = 'DAILY'
}

export enum WaterIntake {
    ONE_TO_THREE_GLASSES = 'ONE_TO_THREE_GLASSES',
    FOUR_TO_SIX_GLASSES = 'FOUR_TO_SIX_GLASSES',
    SEVEN_TO_NINE_GLASSES = 'SEVEN_TO_NINE_GLASSES',
    MORE_THAN_NINE_GLASSES = 'MORE_THAN_NINE_GLASSES'
}

export enum DietType {
    OMNIVORE = 'OMNIVORE',
    VEGETARIAN = 'VEGETARIAN',
    VEGAN = 'VEGAN',
    PESCATARIAN = 'PESCATARIAN',
    GLUTEN_FREE = 'GLUTEN_FREE',
    OTHER = 'OTHER'
}

export enum PreferredContentType {
    ARTICLES = 'ARTICLES',
    SHORT_TIPS = 'SHORT_TIPS',
    MIXED = 'MIXED'
}

export enum ContentFrequency {
    DAILY = 'DAILY',
    FEW_TIMES_PER_WEEK = 'FEW_TIMES_PER_WEEK',
    RARELY = 'RARELY'
}

export enum YesNoOption {
    NO = 'NO',
    YES = 'YES'
}

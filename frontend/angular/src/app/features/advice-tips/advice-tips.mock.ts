export type UserLabel = string;

export interface AdviceSection {
    id: string;
    title: string;
    subtitle: string;
    icon: string;
    imageClass: string;
    matchedLabels: UserLabel[];
    labelWeights: Record<UserLabel, number>;
    preferenceBoostLabels?: UserLabel[];
    safetyPenaltyLabels?: UserLabel[];
    tips: AdviceTip[];
}

export interface AdviceTip {
    id: string;
    title: string;
    summary: string;
    content: string;
}

export interface AdviceTipView extends AdviceTip {
    icon: string;
    sectionTitle: string;
    imageClass: string;
    recommendationScore: number;
    matchedLabelCount: number;
}

export interface AdviceInsight {
    id: string;
    title: string;
    value: string;
    description: string;
    icon: string;
    trend?: string;
}

export const ADVICE_INSIGHTS: AdviceInsight[] = [
    {
        id: 'bmi-status',
        title: 'Weight category',
        value: 'Calculated',
        description: 'BMI-related labels help rank nutrition, activity, and weight-management advice.',
        icon: 'monitor_weight',
        trend: 'Scored signal',
    },
    {
        id: 'sleep-duration',
        title: 'Lifestyle profile',
        value: 'Optional',
        description: 'Sleep, screen time, study load, exercise, and hydration labels refine recommendations.',
        icon: 'bedtime',
        trend: 'Preference aware',
    },
    {
        id: 'nutrition-safety',
        title: 'Safety signals',
        value: 'Prioritized',
        description: 'Allergy and chronic-condition labels are placed first before general lifestyle labels.',
        icon: 'health_and_safety',
        trend: 'Safety first',
    },
];

export const ADVICE_SECTIONS: AdviceSection[] = [
    {
        id: 'nutrition-allergies',
        title: 'Nutrition & allergies',
        subtitle: 'Simple food choices based on your nutrition profile.',
        icon: 'restaurant',
        imageClass: 'nutrition-bg',
        matchedLabels: [
            'HAS_FOOD_ALLERGY',
            'ALLERGY_ONION',
            'ALLERGY_PEANUT',
            'ALLERGY_TREE_NUT',
            'ALLERGY_DAIRY',
            'ALLERGY_GLUTEN',
            'ALLERGY_EGG',
            'ALLERGY_FISH',
            'ALLERGY_SHELLFISH',
            'NUTRITION_SENSITIVE',
            'GOAL_EAT_HEALTHIER',
            'OPTIONAL_DIET_VEGETARIAN',
            'OPTIONAL_DIET_VEGAN',
            'OPTIONAL_DIET_GLUTEN_FREE',
            'OPTIONAL_MEALS_ONE_TO_TWO',
            'OPTIONAL_WATER_ONE_TO_THREE_GLASSES',
        ],
        labelWeights: {
            HAS_FOOD_ALLERGY: 1.25,
            ALLERGY_ONION: 1.45,
            ALLERGY_PEANUT: 1.6,
            ALLERGY_TREE_NUT: 1.6,
            ALLERGY_DAIRY: 1.45,
            ALLERGY_GLUTEN: 1.45,
            ALLERGY_EGG: 1.35,
            ALLERGY_FISH: 1.35,
            ALLERGY_SHELLFISH: 1.55,
            NUTRITION_SENSITIVE: 1.4,
            GOAL_EAT_HEALTHIER: 1.15,
            OPTIONAL_DIET_VEGETARIAN: 0.9,
            OPTIONAL_DIET_VEGAN: 0.9,
            OPTIONAL_DIET_GLUTEN_FREE: 1.0,
            OPTIONAL_MEALS_ONE_TO_TWO: 1.0,
            OPTIONAL_WATER_ONE_TO_THREE_GLASSES: 0.85,
        },
        preferenceBoostLabels: ['OPTIONAL_CONTENT_ARTICLES', 'OPTIONAL_CONTENT_SHORT_TIPS'],
        tips: [
            {
                id: 'check-ingredients',
                title: 'Check ingredient lists carefully',
                summary: 'Look for allergy-related ingredients in sauces, soups, and ready meals.',
                content: 'Food allergens can appear in seasoning mixes, broths, sauces, and processed foods. Checking labels helps you avoid accidental exposure.',
            },
            {
                id: 'simple-meals',
                title: 'Prefer simple meals',
                summary: 'Choose meals with fewer ingredients when trying new foods.',
                content: 'Simple meals make it easier to identify what works well for your body and what may trigger discomfort.',
            },
        ],
    },
    {
        id: 'healthy-weight',
        title: 'Healthy weight support',
        subtitle: 'Practical habits to support a healthier weight.',
        icon: 'monitor_weight',
        imageClass: 'weight-bg',
        matchedLabels: [
            'BMI_OVERWEIGHT',
            'BMI_OBESE',
            'BMI_SEVERELY_OBESE',
            'YOUNG_OVERWEIGHT',
            'WEIGHT_HIGH',
            'WEIGHT_VERY_HIGH',
            'GOAL_MANAGE_WEIGHT',
            'GOAL_ALIGNED_MANAGE_WEIGHT',
            'OPTIONAL_EXERCISE_NONE',
            'OPTIONAL_EXERCISE_ONE_TO_TWO_TIMES',
        ],
        labelWeights: {
            BMI_OVERWEIGHT: 1.35,
            BMI_OBESE: 1.6,
            BMI_SEVERELY_OBESE: 1.8,
            YOUNG_OVERWEIGHT: 1.4,
            WEIGHT_HIGH: 1.1,
            WEIGHT_VERY_HIGH: 1.35,
            GOAL_MANAGE_WEIGHT: 1.25,
            GOAL_ALIGNED_MANAGE_WEIGHT: 1.35,
            OPTIONAL_EXERCISE_NONE: 0.85,
            OPTIONAL_EXERCISE_ONE_TO_TWO_TIMES: 0.65,
        },
        safetyPenaltyLabels: ['CHRONIC_HEART_DISEASE', 'CHRONIC_COPD', 'OPTIONAL_HIGH_SURGERY_HISTORY'],
        tips: [
            {
                id: 'protein-breakfast',
                title: 'Add protein to breakfast',
                summary: 'A balanced breakfast can reduce cravings later in the day.',
                content: 'Try adding eggs, Greek yogurt, cottage cheese, tofu, or another protein source to your morning meal.',
            },
            {
                id: 'plate-method',
                title: 'Use the plate method',
                summary: 'Fill half your plate with vegetables, a quarter with protein, and a quarter with carbs.',
                content: 'This is a simple visual method that helps with portions without counting calories.',
            },
        ],
    },
    {
        id: 'sleep-habits',
        title: 'Sleep & daily habits',
        subtitle: 'Small daily routines that support energy and consistency.',
        icon: 'bedtime',
        imageClass: 'sleep-bg',
        matchedLabels: [
            'OPTIONAL_SLEEP_LESS_THAN_6_HOURS',
            'GOAL_SLEEP_BETTER',
            'SLEEP_WITH_CHRONIC',
            'OPTIONAL_STUDY_LOAD_MORE_THAN_4_HOURS',
            'OPTIONAL_SCREEN_TIME_MORE_THAN_6_HOURS',
            'OPTIONAL_COFFEE_FOUR_OR_MORE_CUPS',
            'OPTIONAL_FREQUENCY_DAILY',
            'OPTIONAL_CONTENT_ARTICLES',
        ],
        labelWeights: {
            OPTIONAL_SLEEP_LESS_THAN_6_HOURS: 1.7,
            GOAL_SLEEP_BETTER: 1.35,
            SLEEP_WITH_CHRONIC: 1.55,
            OPTIONAL_STUDY_LOAD_MORE_THAN_4_HOURS: 1.0,
            OPTIONAL_SCREEN_TIME_MORE_THAN_6_HOURS: 1.0,
            OPTIONAL_COFFEE_FOUR_OR_MORE_CUPS: 0.9,
            OPTIONAL_FREQUENCY_DAILY: 0.55,
            OPTIONAL_CONTENT_ARTICLES: 0.45,
        },
        preferenceBoostLabels: ['OPTIONAL_FREQUENCY_DAILY'],
        tips: [
            {
                id: 'sleep-routine',
                title: 'Create a wind-down routine',
                summary: 'Start relaxing 30 minutes before bedtime.',
                content: 'Dim lights, avoid heavy meals, and reduce screen time before bed to help your body prepare for sleep.',
            },
            {
                id: 'daily-small-step',
                title: 'Focus on one daily action',
                summary: 'Choose one small habit to repeat every day.',
                content: 'Daily consistency works better when the action is small, realistic, and easy to repeat.',
            },
        ],
    },
];

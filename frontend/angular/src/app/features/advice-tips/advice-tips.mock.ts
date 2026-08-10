export type UserLabel =
    | 'ALLERGY_ONION'
    | 'HAS_FOOD_ALLERGY'
    | 'BMI_OVERWEIGHT'
    | 'YOUNG_OVERWEIGHT'
    | 'NUTRITION_SENSITIVE'
    | 'WEIGHT_HIGH'
    | 'NO_CHRONIC_CONDITION'
    | 'GOAL_EAT_HEALTHIER'
    | 'AGE_YOUNG_ADULT'
    | 'HEIGHT_TALL'
    | 'GENDER_MALE'
    | 'OPTIONAL_SLEEP_LESS_THAN_6_HOURS'
    | 'OPTIONAL_CONTENT_ARTICLES'
    | 'OPTIONAL_FREQUENCY_DAILY';

export interface AdviceSection {
    id: string;
    title: string;
    subtitle: string;
    icon: string;
    imageClass: string;
    matchedLabels: UserLabel[];
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
        value: 'Overweight',
        description: 'Your profile suggests focusing on gradual, sustainable habits.',
        icon: 'monitor_weight',
        trend: 'Focus area',
    },
    {
        id: 'sleep-duration',
        title: 'Sleep pattern',
        value: '< 6h',
        description: 'Short sleep may affect appetite, energy, and consistency.',
        icon: 'bedtime',
        trend: 'Needs attention',
    },
    {
        id: 'nutrition-safety',
        title: 'Nutrition sensitivity',
        value: 'High',
        description: 'Ingredient awareness is important because of allergy-related labels.',
        icon: 'restaurant',
        trend: 'Important',
    },
];

export const MOCK_USER_LABELS: UserLabel[] = [
    'ALLERGY_ONION',
    'HAS_FOOD_ALLERGY',
    'BMI_OVERWEIGHT',
    'YOUNG_OVERWEIGHT',
    'NUTRITION_SENSITIVE',
    'WEIGHT_HIGH',
    'NO_CHRONIC_CONDITION',
    'GOAL_EAT_HEALTHIER',
    'AGE_YOUNG_ADULT',
    'HEIGHT_TALL',
    'GENDER_MALE',
    'OPTIONAL_SLEEP_LESS_THAN_6_HOURS',
    'OPTIONAL_CONTENT_ARTICLES',
    'OPTIONAL_FREQUENCY_DAILY',
];

export const ADVICE_SECTIONS: AdviceSection[] = [
    {
        id: 'nutrition-allergies',
        title: 'Nutrition & allergies',
        subtitle: 'Simple food choices based on your nutrition profile.',
        icon: 'restaurant',
        imageClass: 'nutrition-bg',
        matchedLabels: [
            'ALLERGY_ONION',
            'HAS_FOOD_ALLERGY',
            'NUTRITION_SENSITIVE',
            'GOAL_EAT_HEALTHIER',
        ],
        tips: [
            {
                id: 'check-ingredients',
                title: 'Check ingredient lists carefully',
                summary: 'Look for onion-based ingredients in sauces, soups, and ready meals.',
                content: 'Onion can appear in seasoning mixes, broths, sauces, and processed foods. Checking labels helps you avoid accidental exposure.',
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
            'YOUNG_OVERWEIGHT',
            'WEIGHT_HIGH',
        ],
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
            'OPTIONAL_FREQUENCY_DAILY',
            'OPTIONAL_CONTENT_ARTICLES',
        ],
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

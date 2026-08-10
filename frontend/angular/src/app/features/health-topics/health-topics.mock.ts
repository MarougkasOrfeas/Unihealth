export type HealthTopicCategory =
    | 'Preventive Care'
    | 'Nutrition'
    | 'Mental Health'
    | 'Fitness'
    | 'Student Health';

export interface HealthTopic {
    id: string;
    title: string;
    category: HealthTopicCategory;
    summary: string;
    overview: string;
    icon: string;
    accent: 'teal' | 'green' | 'blue' | 'coral' | 'violet' | 'amber';
    accentColor: string;
    readTime: string;
    reviewedBy: string;
    updated: string;
    keyActions: string[];
    warningSigns: string[];
    tags: string[];
    quickFacts: {
        label: string;
        value: string;
    }[];
}

export const HEALTH_TOPIC_CATEGORIES: Array<HealthTopicCategory | 'All'> = [
    'All',
    'Preventive Care',
    'Nutrition',
    'Mental Health',
    'Fitness',
    'Student Health',
];

export const HEALTH_TOPICS: HealthTopic[] = [
    {
        id: 'sleep-recovery',
        title: 'Sleep and Recovery',
        category: 'Student Health',
        summary: 'Build a realistic sleep routine that supports focus, mood, and daily energy.',
        overview: 'Consistent sleep is one of the strongest foundations for academic performance and wellbeing. A practical routine focuses on regular timing, reduced late caffeine, screen-light management, and a short wind-down period that is easy to repeat.',
        icon: 'bedtime',
        accent: 'violet',
        accentColor: '#6a5aa8',
        readTime: '5 min read',
        reviewedBy: 'UniHealth clinical education team',
        updated: 'Updated this semester',
        keyActions: [
            'Keep wake-up time within the same 60-minute window most days.',
            'Stop caffeine 6 to 8 hours before bedtime when possible.',
            'Use a 20-minute wind-down routine with dim lighting and low stimulation.',
            'Move intense study blocks away from the final hour before sleep.',
        ],
        warningSigns: [
            'Sleep problems lasting more than three weeks',
            'Daytime sleepiness that affects driving, classes, or work',
            'Loud snoring, choking, or breathing pauses during sleep',
        ],
        tags: ['Sleep hygiene', 'Energy', 'Routine'],
        quickFacts: [
            {label: 'Best first step', value: 'Stable wake time'},
            {label: 'Common trigger', value: 'Late caffeine'},
        ],
    },
    {
        id: 'balanced-plate',
        title: 'Balanced Plate Basics',
        category: 'Nutrition',
        summary: 'Use a simple plate structure to make everyday meals more balanced.',
        overview: 'Balanced meals do not need complicated tracking. A plate-based approach helps students combine vegetables or fruit, protein, whole grains or starchy foods, and healthy fats in a way that supports satiety and steady energy.',
        icon: 'restaurant',
        accent: 'green',
        accentColor: '#27764f',
        readTime: '4 min read',
        reviewedBy: 'Registered nutrition contributor',
        updated: 'Reviewed recently',
        keyActions: [
            'Aim for half the plate as vegetables or fruit when available.',
            'Include a protein source such as eggs, yogurt, beans, fish, chicken, tofu, or legumes.',
            'Choose water regularly and keep sugary drinks occasional.',
            'Plan one backup meal for busy study days.',
        ],
        warningSigns: [
            'Unexplained weight loss or persistent appetite changes',
            'Food restriction that feels difficult to control',
            'Repeated dizziness, fainting, or weakness',
        ],
        tags: ['Meal planning', 'Protein', 'Hydration'],
        quickFacts: [
            {label: 'Planning window', value: '10 minutes'},
            {label: 'Core habit', value: 'Protein each meal'},
        ],
    },
    {
        id: 'stress-reset',
        title: 'Stress Reset Techniques',
        category: 'Mental Health',
        summary: 'Short techniques for reducing acute stress during exams and deadlines.',
        overview: 'Stress is a normal response to pressure, but it becomes harder to manage when it interrupts sleep, concentration, appetite, or relationships. Short grounding practices can lower intensity enough to choose the next useful action.',
        icon: 'psychology',
        accent: 'blue',
        accentColor: '#2c62a2',
        readTime: '6 min read',
        reviewedBy: 'Student wellbeing team',
        updated: 'Updated for exam periods',
        keyActions: [
            'Try paced breathing: inhale for 4 counts and exhale for 6 counts for two minutes.',
            'Use a five-senses grounding check when thoughts feel overwhelming.',
            'Break study work into one visible next task instead of a full-day list.',
            'Talk with a trusted person or campus support service early.',
        ],
        warningSigns: [
            'Panic symptoms that feel unmanageable or frequent',
            'Loss of interest, hopelessness, or withdrawal lasting days',
            'Any thoughts of self-harm or not wanting to be alive',
        ],
        tags: ['Stress', 'Grounding', 'Exams'],
        quickFacts: [
            {label: 'Fast reset', value: '2 minutes'},
            {label: 'Useful skill', value: 'Grounding'},
        ],
    },
    {
        id: 'movement-routine',
        title: 'Beginner Movement Plan',
        category: 'Fitness',
        summary: 'A low-barrier plan for improving activity without needing a gym.',
        overview: 'Movement supports cardiovascular health, mood, sleep, and concentration. The best plan is one that fits the current schedule and can be repeated without soreness, pressure, or expensive equipment.',
        icon: 'directions_walk',
        accent: 'coral',
        accentColor: '#b95745',
        readTime: '5 min read',
        reviewedBy: 'Exercise health contributor',
        updated: 'Updated this month',
        keyActions: [
            'Start with 20 to 30 minutes of brisk walking three days per week.',
            'Add two short strength sessions using squats, wall push-ups, and hip hinges.',
            'Increase only one variable at a time: duration, frequency, or intensity.',
            'Keep rest days planned, especially after new activity.',
        ],
        warningSigns: [
            'Chest pain, fainting, or severe shortness of breath with activity',
            'Joint pain that worsens or changes your walking pattern',
            'Symptoms that continue after stopping exercise',
        ],
        tags: ['Walking', 'Strength', 'Consistency'],
        quickFacts: [
            {label: 'Starter dose', value: '3 days/week'},
            {label: 'Equipment', value: 'None'},
        ],
    },
    {
        id: 'preventive-checks',
        title: 'Preventive Health Checks',
        category: 'Preventive Care',
        summary: 'Understand routine checks that help detect issues early.',
        overview: 'Preventive care includes vaccinations, dental checks, blood pressure measurement, vision care, sexual health screening when relevant, and routine conversations with a healthcare professional based on age, risk, and history.',
        icon: 'health_and_safety',
        accent: 'teal',
        accentColor: '#1f6f78',
        readTime: '7 min read',
        reviewedBy: 'Primary care education team',
        updated: 'Reviewed annually',
        keyActions: [
            'Keep a personal note of vaccinations and previous screening results.',
            'Measure blood pressure periodically, especially with family history or symptoms.',
            'Book dental and vision checks at intervals recommended by your clinician.',
            'Ask which screenings match your age, sex, medical history, and risk factors.',
        ],
        warningSigns: [
            'New symptoms that persist or worsen',
            'Family history of early heart disease, diabetes, or cancer',
            'Missed follow-up after abnormal test results',
        ],
        tags: ['Screening', 'Vaccines', 'Primary care'],
        quickFacts: [
            {label: 'Best record', value: 'Personal health log'},
            {label: 'Main benefit', value: 'Early detection'},
        ],
    },
    {
        id: 'digital-eye-strain',
        title: 'Digital Eye Strain',
        category: 'Student Health',
        summary: 'Reduce screen-related eye discomfort during long study sessions.',
        overview: 'Long screen sessions can contribute to dry eyes, blurred vision, headaches, and neck strain. Small adjustments to lighting, breaks, screen distance, and blinking can reduce discomfort during coursework.',
        icon: 'visibility',
        accent: 'amber',
        accentColor: '#9d6a18',
        readTime: '3 min read',
        reviewedBy: 'Campus health education team',
        updated: 'Reviewed recently',
        keyActions: [
            'Use the 20-20-20 rule during long screen sessions.',
            'Keep the screen slightly below eye level and about an arm length away.',
            'Reduce glare with softer room lighting or screen positioning.',
            'Blink intentionally and consider asking a clinician about dry-eye options.',
        ],
        warningSigns: [
            'Eye pain, sudden vision changes, or light sensitivity',
            'Headaches that are new, severe, or worsening',
            'Symptoms that do not improve after rest and screen adjustments',
        ],
        tags: ['Screens', 'Study habits', 'Vision'],
        quickFacts: [
            {label: 'Break rhythm', value: 'Every 20 min'},
            {label: 'Setup check', value: 'Arm length'},
        ],
    },
];

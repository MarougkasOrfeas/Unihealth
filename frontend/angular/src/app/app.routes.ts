import {Routes} from '@angular/router';
import {UNIHEALTH_CONSTANTS} from "./shared/constants/unihealth.constants";

export const routes: Routes = [
    {path: '', redirectTo: UNIHEALTH_CONSTANTS.ROUTE_HOME, pathMatch: 'full'},
    {
        path: UNIHEALTH_CONSTANTS.ROUTE_HOME,
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
        data: {breadcrumb: 'breadcrumb.home'},
    },
    {
        path: 'health-news',
        data: {breadcrumb: 'breadcrumb.health.news'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () =>
                    import('./features/health-news/health-news').then((m) => m.HealthNews),
                data: {breadcrumb: "breadcrumb.health.news.today"},
            }
        ],
    },
    {
        path: 'users',
        data: {breadcrumb: 'breadcrumb.users'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/users').then((m) => m.Users),
                data: {breadcrumb: 'breadcrumb.overview'},
            },
            {
                path: 'create',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/create-user/create-user').then((m) => m.CreateUser),
                data: {breadcrumb: 'breadcrumb.users.create'},
            }
        ]
    },
    {
        path: 'groups',
        data: {breadcrumb: 'breadcrumb.groups'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/groups/groups').then((m) => m.Groups),
                data: {breadcrumb: 'breadcrumb.overview'},
            }
        ]
    },
    {
        path: 'departments',
        data: {breadcrumb: 'breadcrumb.departments'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/departments/departments').then((m) => m.Departments),
                data: {breadcrumb: 'breadcrumb.overview'},
            }
        ]
    },
    {
        path: 'profile',
        data: {breadcrumb: 'breadcrumb.profile'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
                data: {breadcrumb: 'breadcrumb.overview'},
            },
            {
                path: 'form',
                pathMatch: 'full',
                loadComponent: () => import('./features/profile/complete-health-profile-form/complete-health-profile-form').then((m) => m.CompleteHealthProfileForm),
                data: {breadcrumb: 'breadcrumb.profile.form'},
            }
        ]
    },
    {
        path: 'topics',
        data: {breadcrumb: 'Health Topics'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () =>
                    import('./features/health-topics/health-topics').then((m) => m.HealthTopics),
                data: {breadcrumb: 'Health Topics'},
            }
        ],
    },
    {
        path: 'advice-tips',
        data: {breadcrumb: 'Advice & Tips'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/advice-tips/advice-tips').then((m) => m.AdviceTips),
                data: {breadcrumb: 'Advice & Tips'},
            }
        ]
    },
    {
        path: 'recipes',
        data: {breadcrumb: 'Healthy Recipes', wellnessPage: 'recipes'},
        loadComponent: () =>
            import('./features/wellness-lifestyle/wellness-lifestyle').then((m) => m.WellnessLifestyle),
    },
    {
        path: 'fitness',
        data: {breadcrumb: 'Fitness', wellnessPage: 'fitness'},
        loadComponent: () =>
            import('./features/wellness-lifestyle/wellness-lifestyle').then((m) => m.WellnessLifestyle),
    },
    {
        path: 'mental-wellbeing',
        data: {breadcrumb: 'Mental Wellbeing', wellnessPage: 'mental'},
        loadComponent: () =>
            import('./features/wellness-lifestyle/wellness-lifestyle').then((m) => m.WellnessLifestyle),
    },
    {
        path: 'symptoms',
        data: {breadcrumb: 'breadcrumb.symptoms'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/symptoms/symptoms').then((m) => m.Symptoms),
                data: {breadcrumb: 'breadcrumb.symptoms.az'},
            },
            {
                path: ':slug',
                pathMatch: 'full',
                loadComponent: () => import('./features/symptoms/symptom-details/symptom-details').then((m) => m.SymptomDetails),
                data: {breadcrumb: 'breadcrumb.details'},
            },
        ]
    },
    {
        path: 'unihealth-ai',
        data: {breadcrumb: 'breadcrumb.ai.assist'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/unihealth-ai/unihealth-ai').then((m) => m.UnihealthAi),
                data: {breadcrumb: 'breadcrumb.ai.unihealth'},
            }
        ]
    }

];

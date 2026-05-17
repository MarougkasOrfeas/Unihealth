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

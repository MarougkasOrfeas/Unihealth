import {Routes} from '@angular/router';
import {UNIHEALTH_CONSTANTS} from "./shared/constants/unihealth.constants";

export const routes: Routes = [
    {path: '', redirectTo: UNIHEALTH_CONSTANTS.ROUTE_HOME, pathMatch: 'full'},
    {
        path: UNIHEALTH_CONSTANTS.ROUTE_HOME,
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
        data: {breadcrumb: 'Home'},
    },
    {
        path: 'health-news',
        data: {breadcrumb: 'Health News'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () =>
                    import('./features/health-news/health-news').then((m) => m.HealthNews),
                data: {breadcrumb: "Today's Health News"},
            }
        ],
    },
    {
        path: 'users',
        data: {breadcrumb: 'Users'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/users').then((m) => m.Users),
                data: {breadcrumb: 'Overview'},
            },
            {
                path: 'create',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/create-user/create-user').then((m) => m.CreateUser),
                data: {breadcrumb: 'Create User'},
            }
        ]
    },
    {
        path: 'groups',
        data: {breadcrumb: 'Groups'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/groups/groups').then((m) => m.Groups),
                data: {breadcrumb: 'Overview'},
            }
        ]
    },
    {
        path: 'departments',
        data: {breadcrumb: 'Departments'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/departments/departments').then((m) => m.Departments),
                data: {breadcrumb: 'Overview'},
            }
        ]
    },
    {
        path: 'profile',
        data: {breadcrumb: 'Profile'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
                data: {breadcrumb: 'Overview'},
            },
            {
                path: 'form',
                pathMatch: 'full',
                loadComponent: () => import('./features/profile/complete-health-profile-form/complete-health-profile-form').then((m) => m.CompleteHealthProfileForm),
                data: {breadcrumb: 'My Form'},
            }
        ]
    },
    {
        path: 'symptoms',
        data: {breadcrumb: 'Symptoms'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/symptoms/symptoms').then((m) => m.Symptoms),
                data: {breadcrumb: 'Symptoms A to Z'},
            },
            {
                path: ':slug',
                pathMatch: 'full',
                loadComponent: () => import('./features/symptoms/symptom-details/symptom-details').then((m) => m.SymptomDetails),
                data: {breadcrumb: 'Details'},
            },
        ]
    }

];

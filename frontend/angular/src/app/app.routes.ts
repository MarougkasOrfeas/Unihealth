import {Routes} from '@angular/router';
import {UNIHEALTH_CONSTANTS} from "./shared/constants/unihealth.constants";
import {adminGuard} from "./core/guards/admin.guard";
import {unsavedChangesGuard} from "./core/guards/unsaved-changes.guard";

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
    // Administration. `canActivate` sits on the parent, which Angular resolves before any child,
    // so one entry covers the list, create, detail and edit routes. Without it these screens are
    // reachable by typing the URL — hiding the nav links is not access control.
    // Note: 'create' must stay above ':id', or it would be read as an id.
    {
        path: 'users',
        canActivate: [adminGuard],
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
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.users.create', mode: 'create'},
            },
            {
                path: ':id',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/create-user/create-user').then((m) => m.CreateUser),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.details', mode: 'view'},
            },
            {
                path: ':id/edit',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/create-user/create-user').then((m) => m.CreateUser),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.users.edit', mode: 'edit'},
            },
        ]
    },
    {
        path: 'groups',
        canActivate: [adminGuard],
        data: {breadcrumb: 'breadcrumb.groups'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/groups/groups').then((m) => m.Groups),
                data: {breadcrumb: 'breadcrumb.overview'},
            },
            {
                path: 'create',
                pathMatch: 'full',
                loadComponent: () => import('./features/groups/group-form/group-form').then((m) => m.GroupForm),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.groups.create', mode: 'create'},
            },
            {
                path: ':id',
                pathMatch: 'full',
                loadComponent: () => import('./features/groups/group-form/group-form').then((m) => m.GroupForm),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.details', mode: 'view'},
            },
            {
                path: ':id/edit',
                pathMatch: 'full',
                loadComponent: () => import('./features/groups/group-form/group-form').then((m) => m.GroupForm),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.groups.edit', mode: 'edit'},
            },
        ]
    },
    {
        path: 'departments',
        canActivate: [adminGuard],
        data: {breadcrumb: 'breadcrumb.departments'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/departments/departments').then((m) => m.Departments),
                data: {breadcrumb: 'breadcrumb.overview'},
            },
            {
                path: 'create',
                pathMatch: 'full',
                loadComponent: () => import('./features/departments/department-form/department-form').then((m) => m.DepartmentForm),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.departments.create', mode: 'create'},
            },
            {
                path: ':id',
                pathMatch: 'full',
                loadComponent: () => import('./features/departments/department-form/department-form').then((m) => m.DepartmentForm),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.details', mode: 'view'},
            },
            {
                path: ':id/edit',
                pathMatch: 'full',
                loadComponent: () => import('./features/departments/department-form/department-form').then((m) => m.DepartmentForm),
                canDeactivate: [unsavedChangesGuard],
                data: {breadcrumb: 'breadcrumb.departments.edit', mode: 'edit'},
            },
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
        data: {breadcrumb: 'breadcrumb.topics'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () =>
                    import('./features/health-topics/health-topics').then((m) => m.HealthTopics),
                data: {breadcrumb: 'breadcrumb.topics.overview'},
            },
            {
                path: 'all',
                pathMatch: 'full',
                loadComponent: () =>
                    import('./features/health-topics/all-health-topics/all-health-topics')
                        .then((m) => m.AllHealthTopics),
                data: {breadcrumb: 'breadcrumb.topics.all'},
            }
        ],
    },
    {
        path: 'advice-tips',
        data: {breadcrumb: 'breadcrumb.advice'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/advice-tips/advice-tips').then((m) => m.AdviceTips),
                // A distinct key from the parent's: the breadcrumb builder emits a crumb for both
                // the parent segment and the '' child, so a shared label renders twice.
                data: {breadcrumb: 'breadcrumb.advice.overview'},
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
        path: 'find-doctor',
        data: {breadcrumb: 'breadcrumb.find-care'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/find-care/find-care').then((m) => m.FindCare),
                data: {breadcrumb: 'breadcrumb.find-care.options'},
            },
            {
                path: ':category',
                pathMatch: 'full',
                loadComponent: () => import('./features/find-care/find-care').then((m) => m.FindCare),
                data: {breadcrumb: 'breadcrumb.find-care.mock'},
            },
            {
                path: 'doctors/:specialty',
                pathMatch: 'full',
                loadComponent: () => import('./features/find-care/find-care').then((m) => m.FindCare),
                data: {breadcrumb: 'breadcrumb.find-care.mock'},
            },
        ]
    },
    {
        path: 'conditions',
        data: {breadcrumb: 'breadcrumb.conditions'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/conditions/conditions').then((m) => m.Conditions),
                data: {breadcrumb: 'breadcrumb.conditions.az'},
            },
            {
                path: ':slug',
                pathMatch: 'full',
                loadComponent: () => import('./features/conditions/condition-details/condition-details').then((m) => m.ConditionDetails),
                data: {breadcrumb: 'breadcrumb.details'},
            },
        ]
    },
    // Flat, with no children: one page needs one crumb. Wrapping a single '' child made the
    // breadcrumb builder emit a crumb for the parent segment and another for the child.
    {
        path: 'cookies-policy',
        loadComponent: () => import('./features/legal/cookies-policy/cookies-policy')
            .then((m) => m.CookiesPolicy),
        data: {breadcrumb: 'breadcrumb.cookies'},
    },
    {
        path: 'unihealth-ai',
        loadComponent: () => import('./features/unihealth-ai/unihealth-ai').then((m) => m.UnihealthAi),
        data: {breadcrumb: 'breadcrumb.ai.chat'},
    }

];

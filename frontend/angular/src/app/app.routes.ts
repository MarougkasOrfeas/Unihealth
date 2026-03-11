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
        path: 'users',
        data: {breadcrumb: 'Users'},
        children: [
            {
                path: '',
                pathMatch: 'full',
                loadComponent: () => import('./features/users/users').then((m) => m.Users),
                data: {breadcrumb: 'Overview'},
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
    }

];

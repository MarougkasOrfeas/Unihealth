import { Routes } from '@angular/router';
import {UNIHEALTH_CONSTANTS} from "./shared/constants/unihealth.constants";

export const routes: Routes = [

    {path: '', redirectTo: UNIHEALTH_CONSTANTS.ROUTE_HOME, pathMatch: 'full'},
    {
        path: UNIHEALTH_CONSTANTS.ROUTE_HOME,
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
        data: {breadcrumb: 'Home'},
    },

];

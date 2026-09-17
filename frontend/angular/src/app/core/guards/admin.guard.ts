import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {map} from 'rxjs';
import {PermissionService} from '../auth/permission.service';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';

/**
 * Restricts the administration screens to administrators. Without it `/users`, `/groups` and
 * `/departments` are reachable by typing the URL — hiding the nav links is not access control.
 *
 * Uses the observable rather than the `isAdmin` signal on purpose: the signal is still `false`
 * while the rights matrix is in flight, so a signal-based guard would reject the first navigation.
 */
export const adminGuard: CanActivateFn = () => {
    const permissions = inject(PermissionService);
    const router = inject(Router);

    return permissions.isAdmin$.pipe(
        map((isAdmin) => isAdmin || router.createUrlTree(['/', UNIHEALTH_CONSTANTS.ROUTE_HOME])),
    );
};

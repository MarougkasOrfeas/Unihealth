import {computed, inject, Injectable, Signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {catchError, map, Observable, of, shareReplay, take} from 'rxjs';
import {UserService} from '../../shared/services/user.service';
import {Permission} from '../interface/permission';

/**
 * Single source of truth for what the logged-in user is allowed to do.
 *
 * The rights matrix is fetched once per app load. Putting `shareReplay` on a stored field is the
 * whole point: applying it inside a method that builds a fresh `HttpClient` observable on every
 * call caches nothing, which is why five `*appHasAdminPermission` instances used to mean five
 * requests.
 */
@Injectable({providedIn: 'root'})
export class PermissionService {

    private readonly userService = inject(UserService);

    private readonly permissions$: Observable<readonly string[]> =
        this.userService.getLoggedinUserRightsMatrix().pipe(
            map((matrix) => matrix?.globalPermissions ?? []),
            catchError((err) => {
                console.error('Failed to load the rights matrix', err);
                return of<string[]>([]);
            }),
            shareReplay({bufferSize: 1, refCount: false}),
        );

    private readonly permissions = toSignal(this.permissions$, {initialValue: null});

    /**
     * Synchronous view, for templates and component logic. `false` until the matrix arrives, so
     * admin-only UI stays hidden rather than flashing into view.
     */
    readonly isAdmin: Signal<boolean> = computed(
        () => this.permissions()?.includes(Permission.ADMIN) ?? false,
    );

    /**
     * Awaitable view, for the route guard. A guard must wait for the answer instead of reading
     * {@link isAdmin}, which is still `false` during the very first navigation.
     */
    readonly isAdmin$: Observable<boolean> = this.permissions$.pipe(
        map((permissions) => permissions.includes(Permission.ADMIN)),
        take(1),
    );
}

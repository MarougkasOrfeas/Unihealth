import {inject, Injectable} from '@angular/core';
import {OAuthEvent, OAuthService} from 'angular-oauth2-oidc';
import {catchError, debounceTime, filter, from, mergeMap, Observable, of, Subject} from 'rxjs';
import {environment} from '../../../environments/environment';
import {UserService} from '../../shared/services/user.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AutoLogoutService} from './auto-logout.service';
import {BeforeUnloadService} from '../../shared/services/beforeunload.service';

@Injectable({providedIn: 'root'})
export class AuthService {
    private readonly afterLoginParamName = 'afterLogin';

    private oauthService = inject(OAuthService);
    private beforeUnload = inject(BeforeUnloadService);
    private autoLogoutService: AutoLogoutService;

    private logoutSubject = new Subject<boolean>();
    private refreshSubject = new Subject<void>();
    private initialized = false;

    /**
     * User service inject to manage user update.
     */
    private userService = inject(UserService);

    /**
     * Mat SnackBar inject to show error popup.
     */
    private snackBar = inject(MatSnackBar);

    constructor() {
        this.autoLogoutService = new AutoLogoutService(this);
    }

    /**
     * Initializes authentication when the application starts.
     *
     * - Loads the OIDC discovery document
     * - Attempts to restore an existing login session
     * - Redirects to the login page if no valid access token is found
     * - Sets up token event monitoring and silent refresh
     *
     * @return An observable that emits `true` if the user is logged in, `false` otherwise.
     */
    init(): Observable<boolean> {
        if (this.initialized) {
            return of(this.authenticated);
        }

        this.initialized = true;

        const redirectUrl = new URL(window.location.origin + window.location.pathname);
        redirectUrl.searchParams.set(this.afterLoginParamName, 'true');

        this.oauthService.configure({
            issuer: window.location.origin + '/keycloak/realms/unihealth',
            redirectUri: redirectUrl.toString(),
            clientId: 'unihealth-client',
            responseType: 'code',
            scope: 'openid profile',
            showDebugInformation: !environment.production,
            requireHttps: false,
            skipIssuerCheck: false,
            strictDiscoveryDocumentValidation: true,
            useSilentRefresh: false,
            clearHashAfterLogin: true,
            sessionChecksEnabled: true,
        });

        return from(this.oauthService.loadDiscoveryDocumentAndTryLogin()).pipe(
            mergeMap(() => {
                if (this.oauthService.hasValidAccessToken()) {
                    this.autoLogoutService.setupCrossTabAuthSync();
                    this.setupTokenEventHandlers();
                    this.autoLogoutService.scheduleTokenExpirationWarning();

                    this.logoutSubject
                        .pipe(debounceTime(100))
                        .subscribe(skipBeforeUnload => this.doLogout(skipBeforeUnload));

                    this.refreshSubject
                        .pipe(debounceTime(1000))
                        .subscribe(() => this.doRefresh());


                    const url = new URL(window.location.href);
                    const isAfterLogin = url.searchParams.get(this.afterLoginParamName) === 'true';

                    if (isAfterLogin) {
                        // return this.userService.justLoggedIn().pipe(
                        //     map(() => {
                        //       url.searchParams.delete(this.afterLoginParamName);
                        //       window.history.replaceState({}, '', url.toString());
                        //       return true;
                        //     }),
                        //     catchError(() => {
                        //       return of(false);
                        //     }),
                        // );
                        return of(false);
                    } else {
                        return of(true);
                    }
                } else {
                    this.oauthService.initCodeFlow();
                    return of(false);
                }
            }),
            catchError(() => {
                this.oauthService.initCodeFlow();
                return of(false);
            }),
        );
    }

    private setupTokenEventHandlers(): void {
        this.oauthService.events.pipe(
            filter((e: OAuthEvent) =>
                e.type === 'silent_refresh_error' ||
                e.type === 'token_refresh_error' ||
                e.type === 'token_error')
        ).subscribe(() => this.logout());

        this.oauthService.events.pipe(
            filter((e: OAuthEvent) => e.type === 'token_received')
        ).subscribe(() => {
            this.autoLogoutService.closeTokenWarningDialog();
            this.autoLogoutService.scheduleTokenExpirationWarning();
            this.autoLogoutService.publishAuthSyncEvent('token_refreshed');
        });
    }

    /**
     * Logs the user out and clears authentication state.
     * Performs both local and server-side logout.
     */
    logout(skipBeforeUnload = true): void {
        try {
            this.autoLogoutService.publishAuthSyncEvent('logout');
        } catch {
            // Ignore cross-tab sync failures and continue with local logout.
        }

        this.performLogout(skipBeforeUnload);
    }

    performLogout(skipBeforeUnload = true): void {
        this.logoutSubject.next(skipBeforeUnload);
    }

    private doLogout(skipBeforeUnload: boolean): void {
        if (skipBeforeUnload) {
            this.beforeUnload.skipOnce();
        }

        this.autoLogoutService.clearTokenExpirationWarningTimer();
        this.autoLogoutService.clearTokenWarningCountdown();
        this.autoLogoutService.clearTokenWarningLogoutTimer();
        this.autoLogoutService.closeTokenWarningDialog();

        try {
            void this.oauthService.revokeTokenAndLogout().catch(() => this.oauthService.logOut());
        } catch (error) {
            this.oauthService.logOut();
        }

        this.oauthService.initCodeFlow();
    }

    refresh(now = false): void {
        if (now) {
            this.doRefresh();
        } else {
            this.refreshSubject.next();
        }
    }

    private doRefresh(): void {
        try {
            void this.oauthService.refreshToken().catch(() => this.logout());
        } catch (error) {
            this.logout();
        }
    }

    get accessToken(): string {
        return this.oauthService.getAccessToken();
    }

    get authenticated(): boolean {
        return this.oauthService.hasValidAccessToken();
    }

    /**
     * Returns the tenant (mandant) of the currently authenticated user
     * from the Keycloak JWT token claim 'tenant'.
     */
    get currentTenant(): string {
        const userDetails = this.oauthService.getIdentityClaims() as {
            tenant?: string;
        } | null;
        if (!userDetails) return '';
        return userDetails.tenant || '';
    }

    /**
     * Returns the username of the currently authenticated user.
     * Falls back to the user's first name if the username is not available. Return null if both fail.
     */
    get username(): string {
        // obtain username and username from keycloak based on client id.
        const userDetails = this.oauthService.getIdentityClaims() as {
            preferred_username?: string;
            given_name?: string;
        } | null;
        // Early return if the user is not authenticated or object could not be retrieved
        if (!userDetails) return '';
        // return username or fallback to first name or null if both fail
        return userDetails.preferred_username || userDetails.given_name || '';
    }
}

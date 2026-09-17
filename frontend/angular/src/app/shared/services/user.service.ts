import {Injectable} from '@angular/core';
import {BaseService} from './base.service';
import {RightsMatrix, User, UserPreferences, UserStatus} from '../interfaces/user';
import {Observable} from "rxjs";
import {UNIHEALTH_CONSTANTS} from "../constants/unihealth.constants";

class UserEndpoints {
    /**
     * Represents the endpoint of a logged-in user.
     */
    static readonly JUST_LOGGED_IN_USER_URI = BaseService.CONTEXT_PATH + `${UNIHEALTH_CONSTANTS.USER_API.JUST_LOGGED_IN}`;
    /**
     * Represents the endpoint to set the user to either ACTIVE (or under circumstances to UNVERIFIED) or DEACTIVATED.
     */
    static readonly SET_USER_STATUS_URI = BaseService.CONTEXT_PATH + `${UNIHEALTH_CONSTANTS.USER_API.SET_USER_STATUS}`;

    static readonly SUGGEST_USERNAME_URI = BaseService.CONTEXT_PATH + `${UNIHEALTH_CONSTANTS.USER_API.SUGGEST_USERNAME}`;

    static readonly CHECK_USERNAME_EXISTS_URI = BaseService.CONTEXT_PATH + `${UNIHEALTH_CONSTANTS.USER_API.CHECK_USERNAME_EXISTS}`;

    static readonly GET_USER_RIGHTS_MATRIX_URI = BaseService.CONTEXT_PATH + '/user/_self/_rights-matrix';
}

type SetUserStatusAnswer = {
    userStatus: string;
    reasonForStatusChange?: string | null;
}

@Injectable({providedIn: 'root'})
export class UserService extends BaseService<User> {

    constructor() {
        super('user');
    }

    justLoggedIn(): Observable<string> {
        return this.httpClient.put(UserEndpoints.JUST_LOGGED_IN_USER_URI, {responseType: 'text',}) as Observable<string>;
    }

    isHealthProfileCompleted(): Observable<boolean> {
        return this.httpClient.get<boolean>(`${this.basePath}/_health_profile_completed`);
    }

    getUser(): Observable<User> {
        return this.httpClient.get<User>(`${this.basePath}/_me`);
    }

    /**
     * Callers should go through `PermissionService`, which owns the caching. A `shareReplay` here
     * would do nothing — the operator would be applied to a fresh request on every call.
     */
    getLoggedinUserRightsMatrix(): Observable<RightsMatrix> {
        return this.httpClient.get<RightsMatrix>(UserEndpoints.GET_USER_RIGHTS_MATRIX_URI);
    }

    setUserStatus(id: string, newUserStatus: UserStatus, deactivationReason: string | null): Observable<SetUserStatusAnswer> {
        return this.httpClient.put<SetUserStatusAnswer>(UserEndpoints.SET_USER_STATUS_URI, {
            id: id,
            userStatus: newUserStatus,
            reason: deactivationReason
        })
    }

    suggestUsername(firstname: string, lastname: string): Observable<string> {
        return this.httpClient.post<string>(UserEndpoints.SUGGEST_USERNAME_URI, {
            firstname,
            lastname
        });
    }

    checkUsernameExists(username: string): Observable<boolean> {
        return this.httpClient.get<boolean>(`${UserEndpoints.CHECK_USERNAME_EXISTS_URI}?username=${username}`);
    }

    /**
     * Whether deactivating or deleting this user would leave the application with no administrator
     * who can sign in. Used to disable the status toggle on the edit page.
     */
    isLastAdmin(id: string): Observable<boolean> {
        return this.httpClient.get<boolean>(`${this.basePath}/${id}/_last_admin`);
    }

    /**
     * The logged-in user's own email preferences. Self-scoped, so it needs no admin permission —
     * the admin-guarded user update cannot be reached through it.
     */
    getMyPreferences(): Observable<UserPreferences> {
        return this.httpClient.get<UserPreferences>(`${this.basePath}/_self/_preferences`);
    }

    /**
     * Saves them. Unsubscribing from the digest also queues the goodbye email server-side, but only
     * on the subscribed -> unsubscribed transition.
     */
    updateMyPreferences(preferences: UserPreferences): Observable<UserPreferences> {
        return this.httpClient.put<UserPreferences>(`${this.basePath}/_self/_preferences`, preferences);
    }

}

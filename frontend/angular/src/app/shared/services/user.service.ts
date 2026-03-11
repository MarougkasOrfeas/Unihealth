import {Injectable} from '@angular/core';
import {BaseService} from './base.service';
import {User, UserStatus} from '../interfaces/user';
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

    setUserStatus(id: string, newUserStatus: UserStatus, deactivationReason: string | null): Observable<SetUserStatusAnswer> {
        return this.httpClient.put<SetUserStatusAnswer>(UserEndpoints.SET_USER_STATUS_URI, {
            id: id,
            userStatus: newUserStatus,
            reason: deactivationReason
        })
    }

    suggestUsername(firstname: string, lastname: string): Observable<string> {
        return this.httpClient.post(UserEndpoints.SUGGEST_USERNAME_URI, {
            firstname,
            lastname
        }, {responseType: 'text' as const});
    }

    checkUsernameExists(username: string): Observable<boolean> {
        return this.httpClient.get<boolean>(`${UserEndpoints.CHECK_USERNAME_EXISTS_URI}?username=${username}`);
    }

}

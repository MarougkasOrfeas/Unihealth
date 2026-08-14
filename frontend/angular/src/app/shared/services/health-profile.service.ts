import {Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';
import {BaseService} from './base.service';
import {HealthProfileDTO, HealthProfileViewDTO} from "../interfaces/health-profile";
import {UserProfileLabel} from "../interfaces/user-profile-label";

class HealthProfileEndpoints {
    static readonly COMPLETE_PROFILE_API = BaseService.CONTEXT_PATH + '/profile/_complete';
    static readonly USER_PROFILE_API = BaseService.CONTEXT_PATH + '/profile/_me';
}


@Injectable({
    providedIn: 'root'
})
export class HealthProfileService extends BaseService<HealthProfileDTO> {

    constructor() {
        super('profile');
    }

    completeProfile(dto: HealthProfileDTO) {
        return this.httpClient.post<void>(
            HealthProfileEndpoints.COMPLETE_PROFILE_API,
            dto,
            {responseType: 'text' as 'json'}
        );
    }

    getMyProfile(): Observable<HealthProfileViewDTO> {
        return this.httpClient.get<HealthProfileViewDTO>(HealthProfileEndpoints.USER_PROFILE_API).pipe(
            map(item => {
                this.transform(item);
                return item;
            })
        );
    }

    updateMyProfile(dto: HealthProfileDTO): Observable<HealthProfileViewDTO> {
        return this.httpClient.put<HealthProfileViewDTO>(HealthProfileEndpoints.USER_PROFILE_API, dto);
    }

    getMyLabels(): Observable<UserProfileLabel[]> {
        return this.httpClient.get<UserProfileLabel[]>(`${HealthProfileEndpoints.USER_PROFILE_API}/labels`);
    }
}

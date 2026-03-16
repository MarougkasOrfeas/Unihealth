import {Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';
import {BaseService} from './base.service';
import {HealthProfileDTO} from "../interfaces/health-profile";

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

    getMyProfile(): Observable<HealthProfileDTO> {
        return this.httpClient.get<HealthProfileDTO>(HealthProfileEndpoints.USER_PROFILE_API).pipe(
            map(item => {
                this.transform(item);
                return item;
            })
        );
    }

    updateMyProfile(dto: HealthProfileDTO): Observable<void> {
        return this.httpClient.put<void>(HealthProfileEndpoints.USER_PROFILE_API, dto);
    }
}

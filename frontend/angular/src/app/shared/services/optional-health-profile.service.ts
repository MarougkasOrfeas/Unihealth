import { Injectable} from '@angular/core';
import { Observable } from 'rxjs';
import {BaseService} from "./base.service";
import {OptionalHealthProfileDto} from "../interfaces/optional-health-profile";

@Injectable({
    providedIn: 'root'
})
export class OptionalHealthProfileService extends BaseService<OptionalHealthProfileDto>{

    constructor() {
        super('profile');
    }

    getMyOptionalProfile(): Observable<OptionalHealthProfileDto> {
        return this.httpClient.get<OptionalHealthProfileDto>(`${this.basePath}/_me/optional`);
    }

    updateMyOptionalProfile(dto: OptionalHealthProfileDto): Observable<void> {
        return this.httpClient.put<void>(`${this.basePath}/_me/optional`, dto);
    }
}

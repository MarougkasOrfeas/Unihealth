import {Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {ConditionItem} from "../interfaces/condition-item";
import {ConditionDetail} from "../interfaces/condition-detail";
import {BaseService} from "./base.service";

class ConditionEndpoints {
    static readonly FIND_CONTENTS_URI = BaseService.CONTEXT_PATH + '/conditions/_content';
    static readonly FIND_BY_SLUG_URI = BaseService.CONTEXT_PATH + '/conditions/_by_slug';
}

@Injectable({providedIn: 'root'})
export class ConditionService extends BaseService<ConditionItem> {

    constructor() {
        super('conditions');
    }

    findContent(search = ''): Observable<ConditionItem[]> {
        return this.httpClient.post<ConditionItem[]>(
            ConditionEndpoints.FIND_CONTENTS_URI,
            {search}
        );
    }

    findBySlug(slug: string): Observable<ConditionDetail> {
        return this.httpClient.post<ConditionDetail>(
            ConditionEndpoints.FIND_BY_SLUG_URI,
            {slug}
        );
    }
}

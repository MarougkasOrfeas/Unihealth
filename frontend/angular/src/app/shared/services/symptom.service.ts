import {Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {SymptomItem} from "../interfaces/symptom-item";
import {SymptomItemDetail} from "../interfaces/symptom-item-detail";
import {BaseService} from "./base.service";

class SymptomEndpoints {
    static readonly FIND_CONTENTS_URI = BaseService.CONTEXT_PATH + '/symptoms/_content';
    static readonly FIND_BY_SLUG_URI = BaseService.CONTEXT_PATH + '/symptoms/_by_slug';
}

@Injectable({providedIn: 'root'})
export class SymptomService extends BaseService<SymptomItem> {

    constructor() {
        super('user');
    }

    findContent(search = ''): Observable<SymptomItem[]> {
        return this.httpClient.post<SymptomItem[]>(
            SymptomEndpoints.FIND_CONTENTS_URI,
            {search}
        );
    }

    findBySlug(slug: string): Observable<SymptomItemDetail> {
        return this.httpClient.post<SymptomItemDetail>(
            SymptomEndpoints.FIND_BY_SLUG_URI,
            {slug}
        );
    }
}

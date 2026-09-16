import {Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {SymptomItem} from "../interfaces/symptom-item";
import {SymptomItemDetail} from "../interfaces/symptom-item-detail";
import {SymptomFactor} from "../interfaces/symptom-factor";
import {PossibleCause} from "../interfaces/possible-cause";
import {BaseService} from "./base.service";

class SymptomEndpoints {
    static readonly FIND_CONTENTS_URI = BaseService.CONTEXT_PATH + '/symptoms/_content';
    static readonly FIND_BY_SLUG_URI = BaseService.CONTEXT_PATH + '/symptoms/_by_slug';
    static readonly FIND_FACTORS_URI = BaseService.CONTEXT_PATH + '/symptoms/_factors';
    static readonly FIND_POSSIBLE_CAUSES_URI = BaseService.CONTEXT_PATH + '/symptoms/_possible_causes';
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

    /** The details a reader can tick for a symptom. Empty when the source does not break it down. */
    findFactors(slug: string): Observable<SymptomFactor[]> {
        return this.httpClient.post<SymptomFactor[]>(
            SymptomEndpoints.FIND_FACTORS_URI,
            {slug}
        );
    }

    /** Ranks the published causes of a symptom against the details the reader ticked. */
    findPossibleCauses(slug: string, factorCodes: string[]): Observable<PossibleCause[]> {
        return this.httpClient.post<PossibleCause[]>(
            SymptomEndpoints.FIND_POSSIBLE_CAUSES_URI,
            {slug, factorCodes}
        );
    }
}

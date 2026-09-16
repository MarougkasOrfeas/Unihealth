import {Injectable, inject} from '@angular/core';
import {Observable, shareReplay} from "rxjs";
import {HttpClient} from "@angular/common/http";
import {DataSource} from "../interfaces/data-source";
import {BaseService} from "./base.service";

class DataSourceEndpoints {
    static readonly FIND_CONTENTS_URI = BaseService.CONTEXT_PATH + '/sources/_content';
}

/**
 * The sources the health content was ingested from, for the credits shown alongside it.
 *
 * <p>The list changes about once a year, and several pages show it at once, so the response is
 * shared rather than refetched per component.
 */
@Injectable({providedIn: 'root'})
export class DataSourceService {

    private httpClient = inject(HttpClient);

    private sources$?: Observable<DataSource[]>;

    findContent(): Observable<DataSource[]> {
        if (!this.sources$) {
            this.sources$ = this.httpClient
                .post<DataSource[]>(DataSourceEndpoints.FIND_CONTENTS_URI, {})
                .pipe(shareReplay({bufferSize: 1, refCount: false}));
        }
        return this.sources$;
    }
}

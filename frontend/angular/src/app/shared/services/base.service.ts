import {UNIHEALTH_CONSTANTS} from '../constants/unihealth.constants';
import {inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {Page} from '../interfaces/page';

export abstract class BaseService<T> {

    /**
     * Represents the root context path of UniHealth.
     */
    static readonly CONTEXT_PATH = `${UNIHEALTH_CONSTANTS.UNIHEALTH_API}`;

    protected httpClient = inject(HttpClient);

    protected basePath: string;

    constructor(endpoint: string) {
        this.basePath = `${BaseService.CONTEXT_PATH}/${endpoint}`;
    }

    create(object: T): Observable<string> {
        return this.httpClient.post<string>(this.basePath, object, {responseType: 'text' as 'json'});
    }

    update(id: string, object: T): Observable<any> {
        return this.httpClient.put<T>(`${this.basePath}/${id}`, object);
    }

    delete(id?: string): Observable<any> {
        return this.httpClient.delete<any>(`${this.basePath}/${id}`);
    }

    get(id: string): Observable<T> {
        return this.httpClient.get<T>(`${this.basePath}/${id}`).pipe(
            map(item => {
                this.transform(item);
                return item
            }));
    }

    getPage(body: Record<string, unknown> = {}): Observable<Page<T>> {
        return this.httpClient.post<Page<T>>(`${this.basePath}/_page`, body).pipe(
            map(page => {
                page.content?.forEach(item => this.transform(item));
                return page;
            })
        );
    }

    protected transform(item: T): void {
        if (!item) {
            return;
        }

        this.transformDates(item);
    }

    private transformDates(item: any): void {
        if (!item) {
            return;
        }

        Object.keys(item).forEach(key => {
            const value: string = item[key] + '';
            if (value?.match('^\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d')) {
                const utcValue = value.endsWith('Z') || /[+-]\d{2}:\d{2}$/.test(value) ? value : value + 'Z';
                const date = new Date(utcValue);
                if (!isNaN(date.getTime())) {
                    item[key] = date;
                }
            }
        });
    }
}

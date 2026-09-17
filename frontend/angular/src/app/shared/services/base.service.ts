import {UNIHEALTH_CONSTANTS} from '../constants/unihealth.constants';
import {inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable, of} from 'rxjs';
import {Page} from '../interfaces/page';
import {TranslateService} from "@ngx-translate/core";
import {BaseFacetEntity} from "../interfaces/baseEntity";
import {ExcelDownloadResponseDTO} from "../interfaces/excel-download-response";

export abstract class BaseService<TRead, TWrite = TRead> {

    /**
     * Represents the root context path of UniHealth.
     */
    static readonly CONTEXT_PATH = `${UNIHEALTH_CONSTANTS.UNIHEALTH_API}`;

    protected httpClient = inject(HttpClient);

    protected basePath: string;

    private translateService = inject(TranslateService);

    constructor(endpoint: string) {
        this.basePath = `${BaseService.CONTEXT_PATH}/${endpoint}`;
    }

    create(object: TWrite): Observable<string> {
        return this.httpClient.post<string>(this.basePath, object, {responseType: 'text' as 'json'});
    }

    update(id: string, object: TWrite): Observable<any> {
        return this.httpClient.put<TWrite>(`${this.basePath}/${id}`, object);
    }

    delete(id?: string): Observable<any> {
        return this.httpClient.delete<any>(`${this.basePath}/${id}`);
    }

    /**
     * Deletes many resources in one request, so a bulk action stays one transaction on the backend.
     * Prefer this over looping `delete()` — N requests are N transactions and commit independently.
     */
    deleteMultiple(ids: string[]): Observable<void> {
        const uniqueIds = Array.from(new Set(ids.filter((id) => !!id)));
        if (uniqueIds.length === 0) {
            return of(void 0);
        }

        return this.httpClient.delete<void>(this.basePath, {body: uniqueIds});
    }

    get(id: string): Observable<TRead> {
        return this.httpClient.get<TRead>(`${this.basePath}/${id}`).pipe(
            map(item => {
                this.transform(item);
                return item
            }));
    }

    getByIds(ids: string[]): Observable<TRead[]> {
        const uniqueIds = Array.from(new Set(ids.filter((id) => !!id)));
        if (uniqueIds.length === 0) {
            return of([]);
        }

        return this.httpClient.post<TRead[]>(`${this.basePath}/_by_ids`, uniqueIds).pipe(
            map(items => {
                items.forEach(item => this.transform(item));
                return items;
            }),
        );
    }

    getPage(body: Record<string, unknown> = {}): Observable<Page<TRead>> {
        return this.httpClient.post<Page<TRead>>(`${this.basePath}/_page`, {locale: this.translateService.getCurrentLang(), ...body}).pipe(
            map(page => {
                page.content?.forEach(item => this.transform(item));
                return page;
            })
        );
    }

    getFacetOptions(body: BaseFacetEntity): Observable<string[]> {
        return this.httpClient.post<string[]>(`${this.basePath}/_facet`, {...body});
    }

    isAvailable(id: string | null, dto: any): Observable<boolean> {
        const body: any = {...dto};
        delete body.id;
        body.id = id;

        return this.httpClient.post<boolean>(`${this.basePath}/_available`, body);
    }

    /**
     * Exports resources to Excel using current query filters.
     *
     * The backend returns a JSON response with the Excel byte[] (serialized as Base64 by Jackson)
     * and the filename, avoiding issues with Content-Disposition headers.
     *
     * @param body Optional request body with filter/sort/pagination parameters.
     * @param mode
     * @returns Observable that emits the decoded blob and filename.
     */
    getExport(
        body: Record<string, unknown> = {},
        mode?: string
    ): Observable<{ blob: Blob, filename: string }> {
        const exportPath = mode ? `${this.basePath}/_export/${mode}` : `${this.basePath}/_export`;

        return this.httpClient.post<ExcelDownloadResponseDTO>(
            exportPath, {locale: this.translateService.getCurrentLang(), ...body}
        ).pipe(
            map(response => BaseService.decodeBase64ToBlob(response))
        );
    }

    /**
     * Decodes a Base64-encoded response from the backend into a Blob + filename pair.
     * Jackson serialises Java byte[] as Base64, so this is required for all Excel downloads.
     */
    static decodeBase64ToBlob(response: ExcelDownloadResponseDTO): { blob: Blob; filename: string } {
        const blob = BaseService.decodeBase64StringToBlob(response.data, response.contentType);
        return {blob, filename: response.filename};
    }

    static decodeBase64StringToBlob(base64String: string, contentType: string): Blob {
        const binaryString = atob(base64String);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return new Blob([bytes], {type: contentType});
    }

    /**
     * Triggers a browser file download for a given Blob.
     */
    static downloadFile(blob: Blob, filename: string): void {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    }

    protected transform(item: TRead): void {
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

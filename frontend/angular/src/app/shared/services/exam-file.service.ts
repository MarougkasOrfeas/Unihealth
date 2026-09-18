import {HttpEvent, HttpResponse} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {map, Observable, switchMap} from 'rxjs';
import {
    ExamFileDTO,
    ExamFileUploadMeta,
    ExamFileUploadResultDTO,
} from '../interfaces/exam-file';
import {filenameFromContentDisposition, toLocalDateString} from '../utils/file.util';
import {BaseService} from './base.service';

/**
 * The medical test documents of the logged-in student.
 *
 * Every endpoint is scoped to the caller on the backend, so nothing here passes a user id — there is
 * no parameter for one.
 */
@Injectable({providedIn: 'root'})
export class ExamFileService extends BaseService<ExamFileDTO> {

    constructor() {
        super('medical-test-file');
    }

    /**
     * Uploads one file, reporting byte-level progress.
     *
     * `Content-Type` is deliberately not set. The browser has to generate the multipart boundary
     * itself, and setting the header by hand strips it, which makes the backend reject the body.
     *
     * With `observe: 'events'` this emits a progress event repeatedly, and the session interceptor
     * taps every one of them to refresh the token. That is harmless — the refresh is debounced, so
     * the flood collapses into a single call after the upload settles — but it looks alarming in the
     * network tab, so it is worth knowing before anyone goes looking for a bug.
     */
    upload(file: File, meta: ExamFileUploadMeta): Observable<HttpEvent<ExamFileUploadResultDTO>> {
        const form = new FormData();
        form.append('multipartFile', file, file.name);
        form.append('category', meta.category);
        if (meta.examDate) {
            form.append('examDate', toLocalDateString(meta.examDate));
        }

        return this.httpClient.post<ExamFileUploadResultDTO>(`${this.basePath}/_upload`, form, {
            reportProgress: true,
            observe: 'events',
        });
    }

    /**
     * Changes the examination date and category of an existing file, leaving everything else alone.
     *
     * <p>Read then write, and deliberately so. The update endpoint takes a whole DTO and its mapper
     * writes every field it is handed, setting a missing one to null — so a body carrying only these
     * two would blank the file name and mark the record inactive. Re-reading first also means a
     * field this page knows nothing about survives an edit to one it does.
     *
     * <p>The date is sent as a plain calendar string rather than a `Date`. `JSON.stringify` renders
     * a `Date` in UTC, and a date picked at local midnight in Greece lands on the previous day once
     * converted — so an edit would silently move every examination back by one.
     */
    updateMetadata(id: string, meta: ExamFileUploadMeta): Observable<unknown> {
        return this.get(id).pipe(switchMap((dto) => this.httpClient.put(`${this.basePath}/${id}`, {
            ...dto,
            category: meta.category,
            // Explicitly null rather than omitted, so clearing the date clears it on the server too.
            examDate: meta.examDate ? toLocalDateString(meta.examDate) : null,
        })));
    }

    /**
     * Fetches the bytes through `HttpClient` rather than pointing an anchor at the URL.
     *
     * The access token is attached by the OAuth interceptor to `HttpClient` requests only, so a
     * plain `<a download>` would arrive unauthenticated: a 401 that never reaches the session
     * interceptor either, leaving the student with neither a re-login nor an error — just a blank
     * tab or an error body saved under the file's name.
     */
    download(id: string): Observable<{ blob: Blob; filename: string | null }> {
        return this.httpClient
            .get(`${this.basePath}/${id}/_download`, {responseType: 'blob', observe: 'response'})
            .pipe(map((response: HttpResponse<Blob>) => ({
                blob: response.body ?? new Blob(),
                filename: filenameFromContentDisposition(
                    response.headers.get('Content-Disposition')),
            })));
    }
}

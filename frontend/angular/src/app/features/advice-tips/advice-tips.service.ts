import {Injectable} from '@angular/core';
import {delay, Observable, of} from 'rxjs';
import {ADVICE_SECTIONS} from './advice-tips.mock';
import {AdviceSection} from './advice-tips.model';

/**
 * The single source of advice content, and the seam where real content will arrive.
 *
 * As with `HealthTopicsService`, this is deliberately the only file allowed to import
 * `ADVICE_SECTIONS`; swapping the body of `getSections()` for an HTTP call is the whole migration.
 */
@Injectable({providedIn: 'root'})
export class AdviceTipsService {

    /** Asynchronous on purpose so the page's loading and error states are exercised from day one. */
    getSections(): Observable<AdviceSection[]> {
        return of(ADVICE_SECTIONS.filter(section => section.active)).pipe(delay(0));
    }
}

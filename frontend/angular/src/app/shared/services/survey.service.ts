import {Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';
import {SurveyDTO} from '../interfaces/survey';
import {BaseService} from './base.service';

/**
 * The «Έρευνα UniHealth» survey response of the logged-in student.
 *
 * <p>Extends {@link BaseService} only for its base path, `transform` and injected `HttpClient` —
 * the inherited CRUD verbs are never used here, because the backend deliberately exposes no
 * id-addressed endpoints for a resource where the only readable row is your own. Same arrangement as
 * {@link HealthProfileService}.
 */
@Injectable({providedIn: 'root'})
export class SurveyService extends BaseService<SurveyDTO> {

    private static readonly MY_SURVEY_API = BaseService.CONTEXT_PATH + '/survey/_me';

    constructor() {
        super('survey');
    }

    /**
     * The caller's answers.
     *
     * <p>Answers with an empty object rather than a 404 when the survey has not been taken, so the
     * form renders blank instead of showing an error to a first-time visitor. `transform` is applied
     * for `modifiedOn`, which the page shows as "you answered on …".
     */
    getMySurvey(): Observable<SurveyDTO> {
        return this.httpClient.get<SurveyDTO>(SurveyService.MY_SURVEY_API).pipe(
            map((response) => {
                this.transform(response);
                return response;
            }),
        );
    }

    /** Replaces any previous response. There is one per student, by design. */
    saveMySurvey(dto: SurveyDTO): Observable<void> {
        return this.httpClient.put<void>(SurveyService.MY_SURVEY_API, dto);
    }
}

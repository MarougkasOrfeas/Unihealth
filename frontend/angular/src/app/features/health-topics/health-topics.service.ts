import {Injectable} from '@angular/core';
import {delay, Observable, of} from 'rxjs';
import {HEALTH_TOPICS} from './health-topics.mock';
import {HealthTopic} from './health-topics.model';

/**
 * The single source of health topics, and the seam where real content will arrive.
 *
 * This is deliberately the only file in the application allowed to import `HEALTH_TOPICS`. When
 * topic content is ingested into a backend table, the body of `getTopics()` becomes the HTTP call
 * and nothing else changes — see `UnihealthAssistantService.getContent()` for the shape it will
 * take:
 *
 *   return this.httpClient.post<HealthTopic[]>(BaseService.CONTEXT_PATH + '/topic/_content',
 *       {locale: this.translateService.getCurrentLang()});
 */
@Injectable({providedIn: 'root'})
export class HealthTopicsService {

    /**
     * Asynchronous on purpose even while it is backed by a constant: the page's loading and error
     * states are then exercised from day one rather than appearing untested the day the real
     * endpoint lands.
     */
    getTopics(): Observable<HealthTopic[]> {
        return of(HEALTH_TOPICS.filter(topic => topic.active)).pipe(delay(0));
    }
}

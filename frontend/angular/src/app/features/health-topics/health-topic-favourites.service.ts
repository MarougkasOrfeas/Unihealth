import {HttpClient} from '@angular/common/http';
import {inject, Injectable, signal} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {BaseService} from '../../shared/services/base.service';
import {MessageService} from '../../shared/services/message.service';

/**
 * The user's favourite topics, persisted per user in `t_user_favourite_topic` so they survive a
 * browser or device change.
 *
 * Writes are optimistic: the heart flips immediately and reverts only if the request fails, so
 * the control never lags a round trip. The backend `PUT`/`DELETE` are idempotent, which is what
 * makes a rapid double click safe.
 */
@Injectable({providedIn: 'root'})
export class HealthTopicFavouritesService {

    private static readonly BASE_PATH = `${BaseService.CONTEXT_PATH}/topic-favourite/_me`;

    private readonly httpClient = inject(HttpClient);
    private readonly messages = inject(MessageService);
    private readonly translate = inject(TranslateService);

    private readonly ids = signal<ReadonlySet<string>>(new Set<string>());

    /** The favourited topic ids. Empty until {@link load} resolves, and after a failed load. */
    readonly favouriteIds = this.ids.asReadonly();

    load(): void {
        this.httpClient.get<string[]>(HealthTopicFavouritesService.BASE_PATH).subscribe({
            next: ids => this.ids.set(new Set(ids)),
            // Favourites are an enhancement, not the point of the page: a failed load leaves the
            // section hidden rather than blocking the topics the user came for.
            error: error => console.error('Failed to load favourite topics', error),
        });
    }

    isFavourite(topicId: string): boolean {
        return this.ids().has(topicId);
    }

    toggle(topicId: string): void {
        const wasFavourite = this.ids().has(topicId);
        this.apply(topicId, !wasFavourite);

        const request = wasFavourite
            ? this.httpClient.delete<void>(`${HealthTopicFavouritesService.BASE_PATH}/${topicId}`)
            : this.httpClient.put<void>(`${HealthTopicFavouritesService.BASE_PATH}/${topicId}`, {});

        request.subscribe({
            error: error => {
                console.error('Failed to update favourite topic', error);
                this.apply(topicId, wasFavourite);
                this.messages.error(this.translate.instant('topics.favourites.error'));
            },
        });
    }

    private apply(topicId: string, favourite: boolean): void {
        this.ids.update(current => {
            const next = new Set(current);
            if (favourite) {
                next.add(topicId);
            } else {
                next.delete(topicId);
            }
            return next;
        });
    }
}

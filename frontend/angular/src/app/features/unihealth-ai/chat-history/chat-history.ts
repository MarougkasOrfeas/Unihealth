import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {DatePipe} from '@angular/common';
import {TranslatePipe} from '@ngx-translate/core';
import {Button} from '../../../shared/components/button/button';
import {NoDataTable} from '../../../shared/components/no-data-table/no-data-table';
import {AiConversationDto} from '../../../shared/interfaces/unihealth-ai';

/**
 * The rail of past conversations beside the chat.
 *
 * Purely presentational: it holds no conversation, fetches nothing and deletes nothing. Every
 * click leaves as an output and the page decides — which is what lets the page cancel a stream in
 * flight before switching, the one thing this feature gets wrong if it is decided in two places.
 *
 * Not `app-base-table`. That is a `mat-table` with a paginator, facet menus and an export toolbar,
 * and it does not fit in 272px. What is reused instead are its parts: `app-empty-state` for the
 * first visit and `app-button` for the delete affordance, so the rail looks like the rest of the
 * application without dragging a table into a sidebar.
 *
 * @author omaro
 */
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-chat-history',
    standalone: true,
    imports: [DatePipe, TranslatePipe, Button, NoDataTable],
    templateUrl: './chat-history.html',
    styleUrl: './chat-history.scss',
})
export class ChatHistory {

    readonly conversations = input<AiConversationDto[]>([]);

    /** Drawer state, which only means anything below 900px. Above it the rail is always there. */
    readonly open = input(false);

    /** The conversation currently on screen, or null for an unsaved new chat. */
    readonly selectedId = input<string | null>(null);

    /** True while the list itself is being fetched, so the empty state does not flash first. */
    readonly loading = input(false);

    /**
     * Note what is *not* an input here: whether a turn is in flight.
     *
     * A student who asked the wrong thing should be able to walk away from it mid-answer, so
     * nothing in this rail is disabled while the model is working. What that requires is that the
     * page cancel the stream on its way out — `UnihealthAi.abandonTurn()` — or one conversation's
     * answer lands inside another.
     */
    readonly selected = output<string>();
    readonly startNew = output<void>();
    readonly removed = output<AiConversationDto>();
}

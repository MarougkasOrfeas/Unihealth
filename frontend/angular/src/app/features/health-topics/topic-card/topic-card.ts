import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {TranslatePipe} from '@ngx-translate/core';
import {HealthTopic, HealthTopicView} from '../health-topics.model';

/**
 * One topic rendered as a card: title, short text, and a favourite toggle.
 *
 * The card knows nothing about profiling. The suggestion section projects its "because you told
 * us" chips into the `[card-reasons]` slot, so a single component serves both the personalised
 * and the plain cases instead of growing a second variant.
 */
@Component({
    selector: 'app-topic-card',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatIconModule, MatButtonModule, MatTooltipModule, TranslatePipe],
    templateUrl: './topic-card.html',
    styleUrl: './topic-card.scss',
})
export class TopicCard {

    readonly topic = input.required<HealthTopicView>();
    readonly showFavourite = input(true);
    readonly showCategory = input(true);
    /** Renders a "#1" badge; used by the trending section to make the ordering explicit. */
    readonly rank = input<number | null>(null);

    readonly open = output<HealthTopic>();
    /** Emits the whole topic, not just its id: the tracker needs its labels. */
    readonly favouriteToggled = output<HealthTopicView>();
}

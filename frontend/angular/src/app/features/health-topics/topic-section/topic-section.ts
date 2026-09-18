import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';

/**
 * The heading row shared by every section of the topics page: an optional icon, a title, an
 * optional subtitle, and an optional "View all" link on the right. The section's content is
 * projected, so this component stays ignorant of what it wraps.
 */
@Component({
    selector: 'app-topic-section',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatIconModule, RouterLink, TranslatePipe],
    templateUrl: './topic-section.html',
    styleUrl: './topic-section.scss',
})
export class TopicSection {

    readonly titleKey = input.required<string>();
    readonly subtitleKey = input<string>('');
    readonly icon = input<string>('');
    /** When set, a "View all" link is rendered pointing at this route. */
    readonly viewAllLink = input<string | null>(null);
    readonly testId = input<string>('topic-section');
}

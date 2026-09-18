import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';

/**
 * The "UniHealth suggests this for you" header card, shared by health topics and advice.
 *
 * Owns only the chrome — eyebrow, heading, match summary, and the complete-your-profile fallback.
 * The suggested items themselves are projected, so each page keeps its own card component.
 *
 * Copy is addressed by a single `keyPrefix` rather than one input per string: each consumer
 * supplies a parallel block of lexicon keys (`topics.suggested.*`, `advice.suggested.*`), which
 * keeps the two pages' wording structurally identical and this component at four inputs.
 */
@Component({
    selector: 'app-suggestion-hero',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatIconModule, MatButtonModule, RouterLink, TranslatePipe],
    templateUrl: './suggestion-hero.html',
    styleUrl: './suggestion-hero.scss',
})
export class SuggestionHero {

    /** Lexicon prefix, e.g. 'topics.suggested'. See the class comment for the keys it expands to. */
    readonly keyPrefix = input.required<string>();
    /** False when the user has no usable labels, which swaps the copy to the fallback wording. */
    readonly isPersonalised = input.required<boolean>();
    /** Distinct profile signals behind the suggestions, shown in the match summary. */
    readonly matchedSignalCount = input(0);
    /** Shows a quiet note that personalisation is degraded, not that the page failed. */
    readonly hasError = input(false);
    readonly testId = input('suggestion-hero');

    protected readonly titleKey = computed(() =>
        this.isPersonalised() ? `${this.keyPrefix()}.title` : `${this.keyPrefix()}.fallback.title`);

    protected readonly subtitleKey = computed(() =>
        this.isPersonalised()
            ? `${this.keyPrefix()}.subtitle`
            : `${this.keyPrefix()}.fallback.subtitle`);
}

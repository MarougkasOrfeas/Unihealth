import {ChangeDetectionStrategy, Component, computed, inject, input} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {map} from 'rxjs';
import {humaniseLabelCode, profileLabelKey} from '../../utils/label-match.util';

/** More than a few stops being a reason and starts being a list. */
const MAX_REASONS = 3;

/**
 * The "Because you told us: …" chip row that explains why a piece of content was recommended.
 *
 * Shared by health topics and advice, so both pages explain themselves the same way and draw on
 * the same `profile.label.*` wording.
 */
@Component({
    selector: 'app-match-reasons',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [TranslatePipe],
    templateUrl: './match-reasons.html',
    styleUrl: './match-reasons.scss',
})
export class MatchReasons {

    private readonly translate = inject(TranslateService);

    /** Raw label codes the user has, already ordered by their own priority. */
    readonly codes = input.required<readonly string[]>();
    readonly max = input(MAX_REASONS);

    /**
     * Tracked so the chips re-resolve on a language switch. `TranslateService.instant` is not
     * reactive, so without this dependency they would stay frozen in the language that was active
     * when they first rendered.
     */
    private readonly lang = toSignal(
        this.translate.onLangChange.pipe(map(event => event.lang)),
        {initialValue: this.translate.getCurrentLang()},
    );

    /**
     * Falls back to a humanised code when a label has no wording yet, so a newly added backend
     * label degrades to "sleep less than 6 hours" instead of leaking `OPTIONAL_SLEEP_LESS_THAN_6_HOURS`.
     */
    readonly reasons = computed(() => {
        this.lang();

        return this.codes().slice(0, this.max()).map(code => {
            const key = profileLabelKey(code);
            const text = this.translate.instant(key) as string;
            return text === key ? humaniseLabelCode(code) : text;
        });
    });
}

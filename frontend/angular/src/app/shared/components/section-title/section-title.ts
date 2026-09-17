import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {MatDivider} from '@angular/material/divider';
import {TranslatePipe} from '@ngx-translate/core';

/**
 * Titled heading for a section of a form, with the rule beneath it.
 *
 * Every create/edit screen groups its fields under one of these, so the heading style lives here
 * rather than being re-declared per page. It renders no container of its own — the caller decides
 * whether the section sits in a `.form-container` card.
 */
@Component({
    selector: 'app-section-title',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatDivider, TranslatePipe],
    templateUrl: './section-title.html',
    styleUrl: './section-title.scss',
})
export class SectionTitle {
    /** Lexicon key of the heading, so every section title is translatable. */
    readonly titleKey = input.required<string>();
    readonly testId = input<string>('section-title');
}

import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatRadioModule} from '@angular/material/radio';
import {TranslateModule} from '@ngx-translate/core';
import {SelectOption} from '../constant-select/constant-select';

/**
 * A single-choice question with every option visible at once.
 *
 * <p>The API is deliberately identical to {@link ConstantSelectComponent} — same inputs, same
 * meanings, same error convention — so the two are interchangeable and choosing between them is a
 * question of how many options there are, not of how much rewiring it costs. `label` arrives already
 * translated; each option's `label` is a lexicon key.
 *
 * <p>Use this over a select when the options are few and the choice is quick. A ten-question survey
 * is the case it was written for: a dropdown per question turns ten decisions into twenty-plus
 * clicks, and hides the alternatives at the moment somebody is weighing them.
 *
 * <p>`@Input` rather than signal inputs, matching the other form controls in this folder. Mixing the
 * two styles across siblings that are meant to be swapped for one another would cost more in
 * surprise than it gains in modernity.
 */
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-radio-group',
    standalone: true,
    imports: [ReactiveFormsModule, MatRadioModule, TranslateModule],
    templateUrl: './radio-group.html',
    styleUrl: './radio-group.scss',
})
export class RadioGroupComponent<K extends string = string> {

    @Input({required: true}) control!: FormControl<K | null>;
    /** Already-translated question text, as on the sibling controls. */
    @Input({required: true}) label!: string;
    @Input({required: true}) options!: ReadonlyArray<SelectOption<K>>;

    @Input() submitted = false;
    @Input() required = false;
    @Input() testId?: string;

    /**
     * Shown as a badge before the question.
     *
     * Optional: a single question standing on its own does not want a number. It earns its place in
     * a long sequence, where it tells the reader both where they are and how the page is organised.
     */
    @Input() number?: number;

    /**
     * Stack the options instead of laying them out in a row.
     *
     * Defaults to stacked: option text here is a short sentence rather than a word, and a row of
     * sentences wraps unpredictably and becomes hard to scan.
     */
    @Input() layout: 'stacked' | 'inline' = 'stacked';

    /**
     * Errors stay hidden until the student has either touched the field or tried to submit, so an
     * untouched form is not a wall of red. Copied from the select rather than reinvented.
     */
    private shouldShowErrors(): boolean {
        return this.submitted || this.control.touched || this.control.dirty;
    }

    showError(code: string): boolean {
        return this.shouldShowErrors() && this.control.hasError(code);
    }
}

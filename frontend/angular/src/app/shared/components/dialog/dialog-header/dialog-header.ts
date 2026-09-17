import {Component, input, output, ChangeDetectionStrategy} from '@angular/core';
import {Button} from '../../button/button';
import {TranslatePipe} from '@ngx-translate/core';
import {MatChip} from '@angular/material/chips';
import {OverflowTooltipDirective} from '../../../directives/overflow-tooltip.directive';

/**
 * Shared component to handle the structure of the dialog header, which includes a title, an
 * optional subtitle and an optional close icon button.
 *
 * The component emits a `dialogClose` event when the close button is clicked, allowing the
 * parent dialog component to handle the closing logic.
 *
 * @see BaseDialog for an example of how to use this component within a dialog structure.
 */
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-dialog-header',
    standalone: true,
    imports: [Button, TranslatePipe, MatChip, OverflowTooltipDirective, OverflowTooltipDirective],
    templateUrl: './dialog-header.html',
    styleUrl: './dialog-header.scss',
})
export class DialogHeader {
    readonly title = input.required<string>();
    readonly subtitle = input<string>();
    readonly showClose = input(true);
    readonly titleSize = input<'default' | 'small'>('default');
    readonly testId = input<string>('dialog-header');
    hasUnsavedChanges = input<boolean>(false);

    readonly dialogClose = output<void>();
}

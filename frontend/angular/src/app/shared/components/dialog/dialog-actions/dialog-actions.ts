import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { MatDialogActions } from '@angular/material/dialog';

/**
 * Shared component to handle the alignment and spacing of the dialog actions.
 *
 * @see BaseDialog for an example of how to use this component within a dialog structure.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dialog-actions',
  standalone: true,
  imports: [MatDialogActions],
  templateUrl: './dialog-actions.html',
  styleUrl: './dialog-actions.scss',
})
export class DialogActions {
  readonly align = input<'flex-start' | 'flex-end' | 'center' | 'space-between'>('flex-end');
}

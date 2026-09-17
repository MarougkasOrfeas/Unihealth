import { ChangeDetectorRef, Component, inject, TemplateRef, ChangeDetectionStrategy } from '@angular/core';
import { BaseDialogData } from '../../../interfaces/base-dialog.model';
import { MAT_DIALOG_DATA, MatDialogContent, MatDialogRef } from '@angular/material/dialog';
import { NgTemplateOutlet } from '@angular/common';
import { DialogHeader } from '../dialog-header/dialog-header';
import { DialogActions } from '../dialog-actions/dialog-actions';
import { Button } from '../../button/button';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Shared component for simple confirmation dialogs. It renders the following based on the
 * provided data:
 *
 * - A header with title and optionally a close icon button
 * - The content which can be a string or a template reference
 * - Action buttons for cancel and confirm, which can be customized with text and styles
 *
 * @see DialogService for opening this dialog with the appropriate data.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-base-dialog',
  standalone: true,
  imports: [NgTemplateOutlet, MatDialogContent, DialogHeader, DialogActions, Button, TranslatePipe],
  templateUrl: './base-dialog.html',
})
export class BaseDialog {
  readonly data: BaseDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<BaseDialog>);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  /**
   * Requests a change-detection pass. Callers that mutate {@link data} in place (for example a
   * live countdown) must invoke this so the OnPush view re-renders the updated content.
   */
  markForCheck(): void {
    this.changeDetectorRef.markForCheck();
  }

  isString(val: unknown): val is string {
    return typeof val === 'string';
  }

  isTemplate(val: unknown): val is TemplateRef<unknown> {
    return val instanceof TemplateRef;
  }
}

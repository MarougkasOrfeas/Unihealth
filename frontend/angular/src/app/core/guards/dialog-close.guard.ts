import { inject, Signal } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { DialogService } from '../../shared/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';

/**
 * Type that components can implement to prevent accidental closing when there are unsaved
 * changes.
 */
export type PendingChanges = {
  /**
   * A Signal that returns true if the component has unsaved changes.
   * If true, the service will intercept close attempts and show a confirmation.
   */
  hasUnsavedChanges: Signal<boolean>;

  /**
   * Optional: Allows the component to handle its own "Are you sure?" logic instead of using the
   * global service default.
   */
  openCustomConfirmDialog?: () => boolean;
}

/**
 * A Type Guard that checks if an object follows the PendingChanges contract.
 * We check if the property exists AND if it is a function (Signal).
 */
export function isPendingChanges(instance: any): instance is PendingChanges {
  return (
    instance &&
    'hasUnsavedChanges' in instance &&
    typeof instance.hasUnsavedChanges === 'function'
  );
}

/**
 * A Router Guard that prevents the user from navigating away from a page if there is an active
 * dialog with unsaved changes.
 */
export const dialogCloseGuard: CanDeactivateFn<unknown> = (): boolean | Observable<boolean> => {
  const dialogService = inject(DialogService);
  const matDialog = inject(MatDialog);

  // If no dialogs are open, navigation is always allowed
  if (matDialog.openDialogs.length === 0) {
    return true;
  }

  // Check the topmost dialog in the stack
  const topDialog = matDialog.openDialogs[matDialog.openDialogs.length - 1];

  return dialogService.canCloseSafely(topDialog);
};

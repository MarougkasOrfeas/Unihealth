import {Injectable, inject} from '@angular/core';
import {MatDialog, MatDialogConfig, MatDialogRef} from '@angular/material/dialog';
import {ComponentType} from '@angular/cdk/portal';
import {BaseDialog} from '../components/dialog/base-dialog/base-dialog';
import {BaseDialogData} from '../interfaces/base-dialog.model';
import {isPendingChanges, PendingChanges} from '../../core/guards/dialog-close.guard';
import {merge, Observable, Subject} from 'rxjs';
import {filter} from 'rxjs/operators';

type DialogCloseAttemptHandler = {
    onCancel?: () => void;
};

@Injectable({
    providedIn: 'root',
})
export class DialogService {
    private readonly dialog = inject(MatDialog);

    constructor() {
        /**
         * Listens for browser-level events (Refresh, Tab Close, Back/Forward).
         * Browsers will show a standard system alert if we prevent default.
         */
        window.addEventListener('beforeunload', (event) => {
            if (this.isAnyOpenDialogDirty()) {
                event.preventDefault();
                // Required for legacy support in Chrome/Edge
                event.returnValue = true;
            }
        });
    }

    /**
     * We use this method whenever we want to open a "Confirmation" type dialog. It renders a
     * BaseDialog component that handles the expected structure and translation keys for a
     * confirmation dialog.
     *
     * Use this for simple Yes/No questions or "Are you sure?" prompts.
     */
    confirm(data: BaseDialogData, config?: MatDialogConfig): MatDialogRef<BaseDialog, boolean> {
        const defaultConfig: MatDialogConfig = {
            maxWidth: 500,
        };

        return this.dialog.open(BaseDialog, {
            data,
            ...defaultConfig,
            ...config,
        });
    }

    /**
     * We use this method to open feature components inside a dialog and to ensure that we
     * intercept close attempts to check for unsaved changes.
     *
     * Note: This method sets 'disableClose: true' to prevent accidental data loss.
     * It manually handles Backdrop and Escape key events to check the dirty state before allowing
     * the dialog to close.
     *
     * The feature component rendered in the dialog should follow a specific structure when
     * possible (to ensure consistency) by breaking it into the following parts:
     *
     * <app-dialog-header/>
     * <mat-dialog-content>...</mat-dialog-content>
     * <app-dialog-actions>...</app-dialog-actions>
     *
     * @see BaseDialog for an example of this structure.
     */
    open<TComponent, TData = unknown, TResult = unknown>(
        component: ComponentType<TComponent>,
        config?: MatDialogConfig<TData>,
    ): MatDialogRef<TComponent, TResult> {

        // We disable standard closing so our manual Esc/Backdrop listeners can check the guard first
        const dialogRef = this.dialog.open(component, {
            ...config,
            disableClose: true,
        });

        // Combine Backdrop clicks and Escape key presses into a single stream
        merge(
            dialogRef.backdropClick(),
            dialogRef.keydownEvents().pipe(filter((e) => e.key === 'Escape')),
        ).subscribe(() => {
            const instance = dialogRef.componentInstance as DialogCloseAttemptHandler;

            if (typeof instance.onCancel === 'function') {
                instance.onCancel();
                return;
            }

            this.closeDialogIfSafe(dialogRef);
        });

        return dialogRef;
    }

    /**
     * Used by the Router Guard to check if navigation should be blocked.
     * @returns true if the user confirmed they want to leave.
     */
    canCloseSafely(dialogRef: MatDialogRef<unknown>): Observable<boolean> | boolean {
        if (this.isComponentDirty(dialogRef)) {
            return this.showUnsavedChangesConfirmation(dialogRef);
        }

        return true;
    }

    /**
     * This it the primary method to close a dialog that can have unsaved changes. It should be
     * used in the internal close logic (e.g. on cancel) of feature components rendered inside the
     * dialog through the {@link open} method.
     */
    closeDialogIfSafe<TResult>(dialogRef: MatDialogRef<unknown, TResult>, result?: TResult): void {
        const instance = dialogRef.componentInstance;

        if (isPendingChanges(instance) && instance.hasUnsavedChanges()) {
            this.showUnsavedChangesConfirmation(dialogRef, instance, result);
        } else {
            dialogRef.close(result);
        }
    }

    /**
     * This is a simple pre-configured instance of the base confirmation dialog for unsaved
     * changes. It can also be used by components when they want to trigger custom logic based on
     * the afterClosed result.
     */
    openUnsavedChangesConfirmDialog(): MatDialogRef<BaseDialog, boolean> {
        return this.confirm(
            {
                content: 'global.message.discard.content',
                cancelText: 'global.cancel',
                confirmText: 'global.leave',
                isDestructive: true
            },
            {
                disableClose: false,
            },
        );
    }

    private isAnyOpenDialogDirty(): boolean {
        return this.dialog.openDialogs.some(dialogRef => {
            return this.isComponentDirty(dialogRef);
        });
    }

    /**
     * Checks the component has unsaved changes.
     * @returns true if the form is 'dirty' and needs a user confirmation to close.
     */
    private isComponentDirty(dialogRef: MatDialogRef<unknown>): boolean {
        const instance = dialogRef.componentInstance;

        if (isPendingChanges(instance)) {
            return instance.hasUnsavedChanges();
        }

        return false;
    }

    /**
     * Internal helper to orchestrate the "Discard Changes" workflow. Handles both the default
     * confirmation and custom component-specific dialogs.
     */
    private showUnsavedChangesConfirmation<TResult>(
        dialogRef: MatDialogRef<unknown, TResult>,
        component?: PendingChanges,
        result?: TResult
    ): Observable<boolean> {
        const subject = new Subject<boolean>();

        if (component?.openCustomConfirmDialog) {
            subject.next(component.openCustomConfirmDialog());
        } else {
            this.openUnsavedChangesConfirmDialog()
                .afterClosed()
                .subscribe((discard) => {
                    if (discard) {
                        subject.next(true);
                        dialogRef.close(result);
                    } else {
                        subject.next(false);
                    }
                    subject.complete();
                });
        }

        return subject.asObservable();
    }
}

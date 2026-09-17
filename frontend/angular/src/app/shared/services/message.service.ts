import {inject, Injectable} from '@angular/core';
import {MatDialogConfig, MatDialogRef} from '@angular/material/dialog';
import {TranslateService} from '@ngx-translate/core';
import {BaseDialog} from '../components/dialog/base-dialog/base-dialog';
import {UNIHEALTH_CONSTANTS} from "../constants/unihealth.constants";
import {BaseDialogData} from '../interfaces/base-dialog.model';
import {DialogService} from './dialog.service';
import {ToastService} from './toast.service';

/**
 * Central entry point for the homogenised success/error toaster alerts and confirmation modals.
 *
 * All pages should use this service instead of building toast/confirm text by hand, so wording and
 * translations are managed from a single point.
 *
 * The operation templates (`global.message.*`) and entity names (`global.entity.*`) live in the
 * backend QLACK lexicon. Pass an entity key from {@link UNIHEALTH_CONSTANTS.ENTITY}, e.g.:
 *
 *   messages.createSuccess(UNIHEALTH_CONSTANTS.ENTITY.USER);
 *   messages.confirmDelete(UNIHEALTH_CONSTANTS.ENTITY.GROUP).afterClosed()...
 */
@Injectable({providedIn: 'root'})
export class MessageService {
    private readonly toast = inject(ToastService);
    private readonly dialog = inject(DialogService);
    private readonly translate = inject(TranslateService);

    private readonly keys = UNIHEALTH_CONSTANTS.MESSAGE_KEYS;

    // --- Toaster alerts -------------------------------------------------------

    /** "<Entity> successfully created." */
    createSuccess(entityKey: string): void {
        this.toast.success(this.withEntity(this.keys.CREATE_SUCCESS, entityKey));
    }

    /** "Creation failed." */
    createError(): void {
        this.toast.error(this.translate.instant(this.keys.CREATE_ERROR));
    }

    /** "<Entity> successfully updated." */
    updateSuccess(entityKey: string): void {
        this.toast.success(this.withEntity(this.keys.UPDATE_SUCCESS, entityKey));
    }

    /** "Update failed." */
    updateError(): void {
        this.toast.error(this.translate.instant(this.keys.UPDATE_ERROR));
    }

    /** "<Entity> successfully deleted." */
    deleteSuccess(entityKey: string): void {
        this.toast.success(this.withEntity(this.keys.DELETE_SUCCESS, entityKey));
    }

    /** "Deletion failed." */
    deleteError(): void {
        this.toast.error(this.translate.instant(this.keys.DELETE_ERROR));
    }

    /**
     * "<Entity> not found." Shown when a detail page is opened for an entry that no longer exists
     * (e.g. a bookmark of a deleted record answered with 404).
     */
    notFound(entityKey: string): void {
        this.toast.error(this.withEntity(this.keys.NOT_FOUND, entityKey));
    }

    /** Shows a custom success toast with already translated text. */
    success(message: string): void {
        this.toast.success(message);
    }

    /** Shows a custom error toast with already translated text. */
    error(message: string): void {
        this.toast.error(message);
    }

    // --- Confirmation modals --------------------------------------------------

    /** Opens a custom confirmation modal using the shared base dialog. */
    confirm(data: BaseDialogData, config?: MatDialogConfig): MatDialogRef<BaseDialog, boolean> {
        return this.dialog.confirm(data, config);
    }

    /**
     * Opens the standard "delete entry" confirmation modal. The returned dialog ref resolves to
     * `true` when the user confirms the deletion.
     */
    confirmDelete(entityKey: string, config?: MatDialogConfig): MatDialogRef<BaseDialog, boolean> {
        return this.dialog.confirm(
            {
                title: this.withEntity(this.keys.DELETE_CONFIRM_TITLE, entityKey),
                content: this.withEntity(this.keys.DELETE_CONFIRM_CONTENT, entityKey),
                confirmText: this.keys.DELETE_CONFIRM_CONFIRM,
                cancelText: this.translate.instant('global.cancel'),
                isDestructive: true,
            },
            config,
        );
    }

    /**
     * Opens the standard "unsaved changes" confirmation modal. The returned dialog ref resolves to
     * `true` when the user chooses to discard and leave.
     */
    confirmDiscard(): MatDialogRef<BaseDialog, boolean> {
        return this.dialog.openUnsavedChangesConfirmDialog();
    }

    /**
     * Interpolates the entity noun into a `global.message.*` template. Greek and English both read
     * naturally from a single `{{entity}}` placeholder, so no per-entity grammatical particles are
     * needed — that was a German-language requirement of the project this service came from.
     */
    private withEntity(templateKey: string, entityKey: string): string {
        return this.translate.instant(templateKey, {
            entity: this.translate.instant(entityKey),
        });
    }
}

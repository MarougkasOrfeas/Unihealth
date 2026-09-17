import {Injectable, Signal, inject} from '@angular/core';
import {MatSnackBar, MatSnackBarConfig, MatSnackBarRef} from '@angular/material/snack-bar';
import {TranslateService} from '@ngx-translate/core';
import {UNIHEALTH_CONSTANTS} from "../constants/unihealth.constants";
import {ToastProgressData, ToastProgressSnackbar} from './toast-progress-snackbar';

export type ToastType = 'success' | 'error' | 'info' | 'warn';

@Injectable({
    providedIn: 'root',
})
export class ToastService {
    private readonly snackBar = inject(MatSnackBar);
    private readonly translate = inject(TranslateService);

    show(
        message: string,
        type: ToastType = 'success',
        duration: number = UNIHEALTH_CONSTANTS.TOAST.DURATION_MS,
        config?: MatSnackBarConfig,
    ): void {
        this.snackBar.open(message, this.translate.instant(UNIHEALTH_CONSTANTS.TOAST.ACTION_LABEL_KEY), {
            duration,
            horizontalPosition: UNIHEALTH_CONSTANTS.TOAST.HORIZONTAL_POSITION,
            verticalPosition: UNIHEALTH_CONSTANTS.TOAST.VERTICAL_POSITION,
            panelClass: this.getPanelClass(type),
            ...config,
        });
    }

    progress(
        message: string,
        progress: Signal<number | null>,
        config?: MatSnackBarConfig,
    ): MatSnackBarRef<ToastProgressSnackbar> {
        return this.snackBar.openFromComponent(ToastProgressSnackbar, {
            data: {message, progress} as ToastProgressData,
            horizontalPosition: UNIHEALTH_CONSTANTS.TOAST.HORIZONTAL_POSITION,
            verticalPosition: UNIHEALTH_CONSTANTS.TOAST.VERTICAL_POSITION,
            panelClass: this.getPanelClass('info'),
            ...config,
        });
    }

    success(message: string, duration?: number, config?: MatSnackBarConfig): void {
        this.show(message, 'success', duration, config);
    }

    error(message: string, duration?: number, config?: MatSnackBarConfig): void {
        this.show(message, 'error', duration, config);
    }

    info(message: string, isLargeExport?: boolean, config?: MatSnackBarConfig): void {
        const preparingDuration = isLargeExport ? 7000 : 5000;
        this.show(message, 'info', preparingDuration, config);
    }

    warn(message: string, duration?: number, config?: MatSnackBarConfig): void {
        this.show(message, 'warn', duration, config);
    }

    private getPanelClass(type: ToastType): string[] {
        switch (type) {
            case 'success':
                return ['unihealth-snackbar-success'];
            case 'error':
                return ['unihealth-snackbar-error'];
            case 'info':
                return ['unihealth-snackbar-info'];
            case 'warn':
                // 'warning', not 'warn': the stylesheet rule is named for the colour, so the
                // mismatch left warn toasts unstyled.
                return ['unihealth-snackbar-warning'];
            default:
                return [];
        }
    }
}

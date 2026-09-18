import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {AnalyticsConsentService} from '../../services/analytics-consent.service';

/**
 * Asks once whether the user agrees to usage measurement.
 *
 * A bespoke component rather than `BaseDialog`, which always renders a close icon and a Cancel
 * button: consent must be a choice between two equally weighted answers, not a dismissal.
 * Accept and Reject are the same size and neither is styled as the primary action.
 *
 * Opened with `disableClose`, so it has to be answered — but answering "no" is one click, and
 * declining costs the user nothing.
 */
@Component({
    selector: 'app-analytics-consent-dialog',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatDialogModule, MatButtonModule, MatIconModule, RouterLink, TranslatePipe],
    templateUrl: './analytics-consent-dialog.html',
    styleUrl: './analytics-consent-dialog.scss',
})
export class AnalyticsConsentDialog {

    private readonly dialogRef = inject(MatDialogRef<AnalyticsConsentDialog>);
    private readonly consent = inject(AnalyticsConsentService);

    protected readonly saving = signal(false);
    protected readonly failed = signal(false);

    protected accept(): void {
        this.answer(true);
    }

    protected reject(): void {
        this.answer(false);
    }

    /** Stays open on failure: closing would leave the user believing an unsaved answer was stored. */
    private answer(granted: boolean): void {
        if (this.saving()) {
            return;
        }

        this.saving.set(true);
        this.failed.set(false);

        this.consent.save(granted)
            .then(() => this.dialogRef.close(granted))
            .catch(() => {
                this.failed.set(true);
                this.saving.set(false);
            });
    }
}

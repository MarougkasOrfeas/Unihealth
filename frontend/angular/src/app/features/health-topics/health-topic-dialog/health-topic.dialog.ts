import {Component, inject, OnDestroy} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {
    MAT_DIALOG_DATA,
    MatDialog,
    MatDialogConfig,
    MatDialogModule,
    MatDialogRef,
} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe} from '@ngx-translate/core';
import {UsageTrackingService} from '../../../core/services/usage-tracking.service';
import {HealthTopic} from '../health-topics.model';

@Component({
    selector: 'app-health-topic-dialog',
    standalone: true,
    imports: [MatButtonModule, MatDialogModule, MatIconModule, TranslatePipe],
    templateUrl: './health-topic.dialog.html',
    styleUrl: './health-topic.dialog.scss',
})
export class HealthTopicDialog implements OnDestroy {
    readonly data = inject<HealthTopic>(MAT_DIALOG_DATA);

    private readonly tracking = inject(UsageTrackingService);
    /**
     * The dialog's lifetime *is* the view, which is why the measurement lives here rather than
     * around `openHealthTopicDialog` — every call site gets it for free, and it covers Escape and
     * backdrop closes as well as the button.
     */
    private readonly openedAt = Date.now();

    constructor() {
        this.tracking.trackInteraction(this.data.matchedLabels);
    }

    ngOnDestroy(): void {
        this.tracking.trackView(this.data.matchedLabels, Date.now() - this.openedAt);
    }
}

const HEALTH_TOPIC_DIALOG_CONFIG: MatDialogConfig<HealthTopic> = {
    width: '760px',
    maxWidth: '94vw',
    panelClass: 'health-topic-dialog-panel',
};

/**
 * Opens the full text of a topic.
 *
 * Uses `MatDialog` directly rather than `DialogService.open()` on purpose: that wrapper exists to
 * force `disableClose` and intercept Escape so unsaved *form* changes can be confirmed. This is
 * read-only content with no dirty state, so routing through it would buy nothing and only risk
 * Escape behaving unexpectedly.
 */
export function openHealthTopicDialog(
    dialog: MatDialog,
    topic: HealthTopic,
): MatDialogRef<HealthTopicDialog> {
    return dialog.open(HealthTopicDialog, {...HEALTH_TOPIC_DIALOG_CONFIG, data: topic});
}

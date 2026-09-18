import {Component, inject, OnDestroy} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe} from '@ngx-translate/core';
import {UsageTrackingService} from '../../../core/services/usage-tracking.service';
import {AdviceTip} from "../advice-tips.model";

/** What the page hands the dialog: a tip plus the identity of the section it came from. */
export interface AdviceTipDialogData extends AdviceTip {
    icon: string;
    sectionTitle: string;
    /**
     * The owning section's targeting labels. Tips themselves carry none — labels live on the
     * section — so they are passed down explicitly, otherwise a tip view could not be measured.
     */
    sectionLabels: string[];
}

@Component({
    selector: 'app-advice-tip-dialog',
    standalone: true,
    imports: [MatDialogModule, MatButtonModule, MatIconModule, TranslatePipe],
    templateUrl: './advice-tip.dialog.html',
    styleUrl: './advice-tip.dialog.scss',
})
export class AdviceTipDialog implements OnDestroy {
    readonly data = inject<AdviceTipDialogData>(MAT_DIALOG_DATA);

    private readonly tracking = inject(UsageTrackingService);
    /** The dialog's lifetime is the view — see `HealthTopicDialog` for the same reasoning. */
    private readonly openedAt = Date.now();

    constructor() {
        this.tracking.trackInteraction(this.data.sectionLabels);
    }

    ngOnDestroy(): void {
        this.tracking.trackView(this.data.sectionLabels, Date.now() - this.openedAt);
    }
}

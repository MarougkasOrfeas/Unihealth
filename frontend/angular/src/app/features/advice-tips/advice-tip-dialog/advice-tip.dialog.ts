import {Component, inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {AdviceTip, AdviceTipView} from "../advice-tips.mock";

@Component({
    selector: 'app-advice-tip-dialog',
    standalone: true,
    imports: [MatDialogModule, MatButtonModule, MatIconModule],
    templateUrl: './advice-tip.dialog.html',
    styleUrl: './advice-tip.dialog.scss',
})
export class AdviceTipDialog {
    readonly data = inject<AdviceTipView>(MAT_DIALOG_DATA);
}

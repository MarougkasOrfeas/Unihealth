import {Component, inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {WellnessItem} from '../wellness-lifestyle.mock';

@Component({
    selector: 'app-wellness-detail-dialog',
    standalone: true,
    imports: [MatButtonModule, MatDialogModule, MatIconModule],
    templateUrl: './wellness-detail.dialog.html',
    styleUrl: './wellness-detail.dialog.scss',
})
export class WellnessDetailDialog {
    readonly data = inject<WellnessItem>(MAT_DIALOG_DATA);
}

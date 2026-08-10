import {Component, inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {HealthTopic} from '../health-topics.mock';

@Component({
    selector: 'app-health-topic-dialog',
    standalone: true,
    imports: [MatButtonModule, MatDialogModule, MatIconModule],
    templateUrl: './health-topic.dialog.html',
    styleUrl: './health-topic.dialog.scss',
})
export class HealthTopicDialog {
    readonly data = inject<HealthTopic>(MAT_DIALOG_DATA);
}

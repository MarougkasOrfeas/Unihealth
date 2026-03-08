import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIcon} from '@angular/material/icon';
import {MatDivider} from '@angular/material/list';
import {ConfirmDialogData} from '../../interfaces/confirm-dialog';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  standalone: true,
  selector: 'app-confirm-discard-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIcon, MatDivider, TranslateModule],
  templateUrl: 'confirm-dialog.html',
  styleUrl: './confirm-dialog.scss'
})
export class ConfirmationDialog {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {
  }
}

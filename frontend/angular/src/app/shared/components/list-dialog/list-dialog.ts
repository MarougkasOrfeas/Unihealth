import {Component, inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogClose, MatDialogContent, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatDividerModule} from '@angular/material/divider';
import {TranslatePipe} from '@ngx-translate/core';
import {DialogHeader} from "../dialog/dialog-header/dialog-header";
import {DialogActions} from "../dialog/dialog-actions/dialog-actions";
import {Button} from "../button/button";

export type ListDialogData = {
    title: string;
    items: string[];
};

@Component({
    selector: 'app-view-more-list-dialog',
    standalone: true,
    imports: [MatButtonModule, MatIconModule, MatDividerModule, TranslatePipe, DialogHeader, DialogActions, MatDialogContent, Button],
    templateUrl: './list-dialog.html',
    styleUrl: './list-dialog.scss',
})
export class ListDialogComponent {
    readonly data = inject<ListDialogData>(MAT_DIALOG_DATA);
    private readonly dialogRef = inject(MatDialogRef<ListDialogComponent>);

    get title(): string {
        return this.data.title;
    }

    get items(): string[] {
        return this.data.items ?? [];
    }

    close(): void {
        this.dialogRef.close();
    }
}

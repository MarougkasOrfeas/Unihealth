import {Component, ElementRef, EventEmitter, Input, Output, ViewChild, ViewEncapsulation} from '@angular/core';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {TranslatePipe} from '@ngx-translate/core';
import {Button} from '../button/button';

/**
 * "More actions" menu holding Excel export and import.
 *
 * Import is a front-end hook only: it opens the file picker and hands the `change` event to the
 * caller. There is no import endpoint on the backend, so a page opts in only when it can do
 * something with the file.
 */
@Component({
    selector: 'app-import-export-actions-menu',
    encapsulation: ViewEncapsulation.None,
    standalone: true,
    imports: [Button, MatIconModule, MatMenuModule, MatTooltipModule, TranslatePipe],
    templateUrl: './import-export-actions-menu.html',
    styleUrl: './import-export-actions-menu.scss',
})
export class ImportExportActionsMenuComponent {
    @Input() showImport = false;
    @Input() showExport = true;
    @Input() isImporting = false;
    @Input() isExportDisabled = false;
    /** Shows the item but blocks it, e.g. while no importer exists for the entity yet. */
    @Input() isImportDisabled = false;
    @Input() disabled = false;

    @Input() importLabelKey = 'global.action.import';
    @Input() exportLabelKey = 'global.export.action.label';
    @Input() triggerLabelKey = 'global.btn.more.actions';
    @Input() importTooltipKey = 'global.action.import.tooltip';
    @Input() importRunningTooltipKey = 'global.action.import.running';
    @Input() importDisabledTooltipKey = 'global.action.import.unavailable';
    @Input() exportTooltipKey = 'global.action.export.tooltip';
    @Input() exportDisabledTooltipKey = 'global.action.export.disabled';

    @Input() triggerTestId = 'more-actions-button';
    @Input() importTestId = 'btn-import';
    @Input() exportTestId = 'btn-export';

    @Output() importClicked = new EventEmitter<void>();
    @Output() exportClicked = new EventEmitter<void>();
    @Output() importFileSelected = new EventEmitter<Event>();

    @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

    get hasAnyAction(): boolean {
        return this.showImport || this.showExport;
    }

    get importTooltip(): string {
        if (this.isImportDisabled) {
            return this.importDisabledTooltipKey;
        }
        return this.isImporting ? this.importRunningTooltipKey : this.importTooltipKey;
    }

    get exportTooltip(): string {
        return this.isExportDisabled ? this.exportDisabledTooltipKey : this.exportTooltipKey;
    }

    onImport(): void {
        if (this.isImporting || this.isImportDisabled) {
            return;
        }
        // Opened here rather than by the parent, so the caller only has to react to the file.
        this.fileInput?.nativeElement.click();
        this.importClicked.emit();
    }

    onExport(): void {
        this.exportClicked.emit();
    }

    onImportFileSelected(event: Event): void {
        this.importFileSelected.emit(event);
        // Reset, so selecting the same file twice in a row still fires a change event.
        if (this.fileInput) {
            this.fileInput.nativeElement.value = '';
        }
    }
}

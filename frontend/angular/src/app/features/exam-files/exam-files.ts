import {ChangeDetectionStrategy, Component, DestroyRef, inject, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {filter, switchMap} from 'rxjs';
import {BaseTable, EmptyStateData, InlineEdit} from '../../shared/components/base-table/base-table';
import {PageHeader} from '../../shared/components/page-header/page-header';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';
import {ExamCategory, ExamFileDTO} from '../../shared/interfaces/exam-file';
import {BaseService} from '../../shared/services/base.service';
import {ExamFileService} from '../../shared/services/exam-file.service';
import {DialogService} from '../../shared/services/dialog.service';
import {MessageService} from '../../shared/services/message.service';
import {ToastService} from '../../shared/services/toast.service';
import {fileTypeLabel, formatBytes} from '../../shared/utils/file.util';
import {errorMessageFromHttp} from '../../shared/utils/http-error.util';
import {ExamFilePreview, ExamFilePreviewData} from './exam-file-preview/exam-file-preview';
import {ExamFileUpload} from './exam-file-upload/exam-file-upload';
import {EXAM_FILE_COLUMNS, ExamFileRow} from './exam-files.columns';

/** «Οι Εξετάσεις Μου» — a student's own medical test documents. */
@Component({
    selector: 'app-exam-files',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [BaseTable, ExamFileUpload, PageHeader, TranslatePipe],
    templateUrl: './exam-files.html',
    styleUrl: './exam-files.scss',
})
export class ExamFiles {

    private readonly messages = inject(MessageService);
    private readonly dialogs = inject(DialogService);
    private readonly toast = inject(ToastService);
    private readonly translate = inject(TranslateService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly examFileService = inject(ExamFileService);

    // The explicit generics are load-bearing: without them TRow does not infer and the calls to
    // reloadFirstPage below type-check against the wrong instantiation.
    private readonly table = viewChild.required(BaseTable<ExamFileDTO, ExamFileRow>);

    protected readonly columns = EXAM_FILE_COLUMNS;

    protected readonly emptyState: EmptyStateData = {
        titleKey: 'examFile.list.empty.title',
        subtitleKey: 'examFile.list.empty.subtitle',
        titleErrorKey: 'global.list.error.title',
        subtitleErrorKey: 'global.list.error.subtitle',
    };

    constructor() {
        // The category is translated into the row by mapToRow, because the table has no cell type
        // that translates at render time. That means a language switch needs a refetch, or the
        // column keeps showing the previous language.
        this.translate.onLangChange
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.table().loadData());
    }

    protected readonly toRow = (dto: ExamFileDTO): ExamFileRow => ({
        id: dto.id,
        fileType: fileTypeLabel(dto.contentType),
        contentType: dto.contentType,
        name: dto.name,
        category: dto.category ?? null,
        categoryLabel: dto.category
            ? this.translate.instant(`examFile.category.${dto.category}`)
            : '-',
        examDate: dto.examDate ?? null,
        fileSizeLabel: formatBytes(dto.fileSize, this.translate.getCurrentLang()),
    });

    /** A new upload sorts to the top, so the list is reset to the first page rather than reloaded. */
    protected onUploaded(): void {
        this.table().reloadFirstPage();
    }

    /**
     * Opens the document in place.
     *
     * <p>Wired to the table's `view` action rather than to a click on the file name. The actions
     * column is where every other list in this application keeps its per-row verbs, so it is where
     * someone will look; a clickable cell would also fight selecting the name to copy it, and would
     * not be announced as something a screen reader can activate.
     */
    protected onPreview(row: ExamFileRow): void {
        this.dialogs.open<ExamFilePreview, ExamFilePreviewData>(ExamFilePreview, {
            data: {id: row.id, name: row.name, contentType: row.contentType},
            width: '90vw',
            maxWidth: '1100px',
            autoFocus: 'dialog',
        });
    }

    /**
     * Saves an in-place edit. Only the examination date and the category are editable — the file
     * itself, its name and its type are not something an edit should be able to touch.
     */
    protected onSaveEdit({row, values}: InlineEdit<ExamFileRow>): void {
        const category = (values['category'] as ExamCategory | null) ?? 'OTHER';
        const examDate = (values['examDate'] as Date | null) ?? null;

        this.examFileService.updateMetadata(row.id, {category, examDate})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                // The current page, not the first: an edit does not move a row the way a create or
                // a delete does, so the reader keeps their place in the list.
                next: () => {
                    this.messages.updateSuccess(UNIHEALTH_CONSTANTS.ENTITY.EXAM_FILE);
                    this.table().loadData();
                },
                error: (err) => this.toast.error(errorMessageFromHttp(err, this.translate)),
            });
    }

    protected onDownload(row: ExamFileRow): void {
        this.examFileService
            .download(row.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                // The header name when the server sends a usable one, the row's own otherwise —
                // the row already holds the right name, so the header is a nicety rather than a
                // dependency.
                next: ({blob, filename}) => BaseService.downloadFile(blob, filename ?? row.name),
                // No label map: a failed download is a transport or server problem, not one of
                // the business labels the upload path translates.
                error: (err) => this.toast.error(errorMessageFromHttp(err, this.translate)),
            });
    }

    protected onDelete(row: ExamFileRow): void {
        this.messages.confirmDelete(UNIHEALTH_CONSTANTS.ENTITY.EXAM_FILE)
            .afterClosed()
            .pipe(
                filter(Boolean),
                switchMap(() => this.examFileService.delete(row.id)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: () => {
                    this.messages.deleteSuccess(UNIHEALTH_CONSTANTS.ENTITY.EXAM_FILE);
                    this.table().reloadFirstPage();
                },
                error: () => this.messages.deleteError(),
            });
    }
}

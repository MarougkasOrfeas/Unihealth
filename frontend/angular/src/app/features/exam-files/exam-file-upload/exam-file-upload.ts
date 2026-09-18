import {HttpErrorResponse, HttpEventType} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, output, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatIcon} from '@angular/material/icon';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {catchError, concatMap, filter, finalize, from, map, of, tap, toArray} from 'rxjs';
import {Button} from '../../../shared/components/button/button';
import {ConstantSelectComponent} from '../../../shared/components/constant-select/constant-select';
import {DatePickerComponent} from '../../../shared/components/date-picker/date-picker';
import {FileDropZone, FileRejection} from '../../../shared/components/file-drop-zone/file-drop-zone';
import {
    EXAM_CATEGORY_OPTIONS,
    ExamCategory,
} from '../../../shared/interfaces/exam-file';
import {ExamFileService} from '../../../shared/services/exam-file.service';
import {ToastService} from '../../../shared/services/toast.service';
import {
    EXAM_FILE_ACCEPT,
    fileIconFor,
    formatBytes,
    isAcceptedExamFile,
} from '../../../shared/utils/file.util';
import {errorMessageFromHttp} from '../../../shared/utils/http-error.util';

/** Twenty megabytes, matching the backend limit. Rejecting here saves a pointless round trip. */
const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024;

/** Labels the backend may answer with, mapped to something a student can read. */
const EXAM_FILE_ERROR_LABELS: Record<string, string> = {
    'global.file.too.large': 'examFile.error.file_too_large',
    'global.file.unsupported.type': 'examFile.error.unsupported_media_type',
    'global.file.name.too.long': 'examFile.error.name_too_long',
};

type StagedFile = {
    readonly file: File;
    readonly form: FormGroup<{
        examDate: FormControl<Date | null>;
        category: FormControl<ExamCategory>;
    }>;
};

type UploadOutcome = {
    readonly file: File;
    readonly ok: boolean;
    readonly error?: HttpErrorResponse;
};

/**
 * Staging and uploading medical test files.
 *
 * Each file carries its own small form, because a student who drops a blood test and an X-ray
 * together needs to tag them differently — a single form for the batch would force them to upload
 * twice.
 */
@Component({
    selector: 'app-exam-file-upload',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        Button,
        ConstantSelectComponent,
        DatePickerComponent,
        FileDropZone,
        MatIcon,
        ReactiveFormsModule,
        TranslatePipe,
    ],
    templateUrl: './exam-file-upload.html',
    styleUrl: './exam-file-upload.scss',
})
export class ExamFileUpload {

    /** Fired once per batch, when at least one file made it. */
    readonly uploaded = output<void>();

    private readonly examFiles = inject(ExamFileService);
    private readonly toast = inject(ToastService);
    private readonly translate = inject(TranslateService);
    private readonly formBuilder = inject(FormBuilder);
    private readonly destroyRef = inject(DestroyRef);

    protected readonly accept = EXAM_FILE_ACCEPT;
    protected readonly maxSizeBytes = MAX_FILE_SIZE_BYTES;
    protected readonly categoryOptions = EXAM_CATEGORY_OPTIONS;
    protected readonly isAccepted = isAcceptedExamFile;
    protected readonly formatBytes = formatBytes;
    protected readonly fileIconFor = fileIconFor;
    protected readonly today = new Date();

    protected readonly staged = signal<readonly StagedFile[]>([]);
    protected readonly uploading = signal(false);
    protected readonly submitted = signal(false);

    // Three primitives rather than one percentage, so several files can share one bar.
    private readonly completedBytes = signal(0);
    private readonly currentBytes = signal(0);
    private readonly totalBytes = signal(0);

    /**
     * `null` is not "unknown" — it is the sentinel that closes the progress snackbar, so it must be
     * returned whenever nothing is in flight.
     *
     * The denominator is the summed `file.size` rather than the upload event's own `total`, which is
     * undefined until the browser has worked out the encoded body length. Multipart framing adds a
     * few hundred bytes per file, so this under-reports very slightly — invisible once rounded.
     */
    private readonly percent = computed<number | null>(() => {
        if (!this.uploading()) {
            return null;
        }
        const total = this.totalBytes();
        if (total <= 0) {
            return 0;
        }
        return Math.min(100,
            Math.round(((this.completedBytes() + this.currentBytes()) / total) * 100));
    });

    protected onFilesSelected(files: readonly File[]): void {
        const additions = files.map((file) => ({
            file,
            form: this.formBuilder.nonNullable.group({
                examDate: new FormControl<Date | null>(null),
                category: new FormControl<ExamCategory>('OTHER', {
                    nonNullable: true,
                    validators: [Validators.required],
                }),
            }),
        }));

        // Appended rather than replaced, so a second drop adds to the first.
        this.staged.update((current) => [...current, ...additions]);
    }

    protected onFilesRejected(rejections: readonly FileRejection[]): void {
        // The drop zone already lists them inline; nothing more to do than let it.
        void rejections;
    }

    protected remove(target: StagedFile): void {
        this.staged.update((items) => items.filter((item) => item !== target));
    }

    protected clear(): void {
        this.staged.set([]);
        this.submitted.set(false);
    }

    /** Copies the first row's date and category down, so five scans are not five identical forms. */
    protected applyToAll(): void {
        const items = this.staged();
        if (items.length < 2) {
            return;
        }
        const {examDate, category} = items[0].form.getRawValue();
        for (const item of items.slice(1)) {
            item.form.setValue({examDate, category});
        }
    }

    protected submit(): void {
        const staged = this.staged();
        if (!staged.length || this.uploading()) {
            return;
        }
        if (staged.some((item) => item.form.invalid)) {
            staged.forEach((item) => item.form.markAllAsTouched());
            this.submitted.set(true);
            return;
        }

        this.totalBytes.set(staged.reduce((sum, item) => sum + item.file.size, 0));
        this.completedBytes.set(0);
        this.currentBytes.set(0);
        this.uploading.set(true);

        const toastRef = this.toast.progress(
            this.translate.instant('examFile.upload.progress'), this.percent);

        from(staged)
            .pipe(
                // One request at a time. It keeps the aggregate percentage monotonic, avoids
                // holding several multipart bodies in memory at once, and makes a partial failure
                // reportable in a stable order.
                concatMap((item) =>
                    this.examFiles.upload(item.file, item.form.getRawValue()).pipe(
                        tap((event) => {
                            if (event.type === HttpEventType.UploadProgress) {
                                this.currentBytes.set(event.loaded);
                            }
                        }),
                        filter((event) => event.type === HttpEventType.Response),
                        map(() => ({file: item.file, ok: true}) as UploadOutcome),
                        // Caught on the inner stream, so one rejected file cannot cancel the rest
                        // of the batch. On the outer stream it would end everything.
                        catchError((error: HttpErrorResponse) =>
                            of({file: item.file, ok: false, error} as UploadOutcome)),
                        // Runs whether the file succeeded or failed, so the bar never stalls on one
                        // the server turned away.
                        finalize(() => {
                            this.completedBytes.update((done) => done + item.file.size);
                            this.currentBytes.set(0);
                        }),
                    )),
                toArray(),
                finalize(() => {
                    this.uploading.set(false);   // percent() turns null, dismissing the snackbar
                    toastRef.dismiss();          // in case the effect has not flushed yet
                }),
                // Last, so the finalize above still runs if the page is left mid-upload and the
                // progress snackbar cannot outlive it.
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe((outcomes) => this.report(outcomes));
    }

    private report(outcomes: readonly UploadOutcome[]): void {
        const failed = outcomes.filter((outcome) => !outcome.ok);

        if (!failed.length) {
            this.toast.success(this.translate.instant('examFile.upload.success',
                {count: outcomes.length}));
        } else if (failed.length === outcomes.length) {
            // One message taken from the first failure: N identical toasts for N rejected files is
            // noise, not information.
            this.toast.error(
                errorMessageFromHttp(failed[0].error!, this.translate, EXAM_FILE_ERROR_LABELS));
        } else {
            this.toast.warn(this.translate.instant('examFile.upload.partial', {
                count: failed.length,
                names: failed.map((outcome) => outcome.file.name).join(', '),
            }));
        }

        // Successful files leave the list; failed ones stay, so a student can correct the category
        // and try again without hunting for the file a second time.
        const failedFiles = new Set(failed.map((outcome) => outcome.file));
        this.staged.update((items) => items.filter((item) => failedFiles.has(item.file)));
        this.submitted.set(false);

        if (failed.length < outcomes.length) {
            this.uploaded.emit();
        }
    }
}

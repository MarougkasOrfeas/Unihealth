import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatDialogModule, MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {MatIcon} from '@angular/material/icon';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {Button} from '../../../shared/components/button/button';
import {DialogActions} from '../../../shared/components/dialog/dialog-actions/dialog-actions';
import {DialogHeader} from '../../../shared/components/dialog/dialog-header/dialog-header';
import {SpinnerComponent} from '../../../shared/components/spinner/spinner';
import {BaseService} from '../../../shared/services/base.service';
import {ExamFileService} from '../../../shared/services/exam-file.service';
import {ToastService} from '../../../shared/services/toast.service';
import {errorMessageFromHttp} from '../../../shared/utils/http-error.util';

export type ExamFilePreviewData = {
    readonly id: string;
    readonly name: string;
    readonly contentType: string;
};

/** How the body renders once the bytes are here. */
type PreviewKind = 'image' | 'pdf' | 'unsupported';

const IMAGE_TYPES = new Set(['image/jpeg', 'image/png']);

/**
 * Shows an uploaded medical test document without leaving the list.
 *
 * <p>The bytes are fetched through {@link ExamFileService}, not by pointing an `iframe` at the
 * download URL: the access token is attached to `HttpClient` requests only, so a direct URL would
 * load an unauthenticated 401 page inside the dialog.
 */
@Component({
    selector: 'app-exam-file-preview',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        Button,
        DialogActions,
        DialogHeader,
        MatDialogModule,
        MatIcon,
        SpinnerComponent,
        TranslatePipe,
    ],
    templateUrl: './exam-file-preview.html',
    styleUrl: './exam-file-preview.scss',
})
export class ExamFilePreview implements OnInit {

    protected readonly data = inject<ExamFilePreviewData>(MAT_DIALOG_DATA);

    private readonly dialogRef = inject(MatDialogRef<ExamFilePreview>);
    private readonly examFiles = inject(ExamFileService);
    private readonly sanitizer = inject(DomSanitizer);
    private readonly toast = inject(ToastService);
    private readonly translate = inject(TranslateService);
    private readonly destroyRef = inject(DestroyRef);

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);

    /** Held so it can be revoked; an unrevoked object URL pins the whole file in memory. */
    private readonly objectUrl = signal<string | null>(null);
    private blob: Blob | null = null;

    protected readonly imageUrl = computed(() => this.objectUrl());

    /**
     * An `iframe` source is a RESOURCE_URL, which Angular never sanitises automatically — it has to
     * be marked trusted explicitly. Safe here because the value is an object URL this component
     * created itself from a response the user is authorised to read.
     */
    protected readonly pdfUrl = computed<SafeResourceUrl | null>(() => {
        const url = this.objectUrl();
        return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
    });

    /**
     * TIFF is deliberately absent.
     *
     * <p>It is an accepted upload type, but Chrome and Firefox cannot render it — only Safari can.
     * Rather than showing those students a broken image area, it falls through to the unsupported
     * state, which offers the download they would have needed anyway.
     */
    protected readonly kind = computed<PreviewKind>(() => {
        const type = this.data.contentType;
        if (IMAGE_TYPES.has(type)) {
            return 'image';
        }
        return type === 'application/pdf' ? 'pdf' : 'unsupported';
    });

    ngOnInit(): void {
        // Nothing to fetch if it cannot be shown; the dialog offers the download instead.
        if (this.kind() === 'unsupported') {
            this.loading.set(false);
            return;
        }

        this.examFiles.download(this.data.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: ({blob}) => {
                    this.blob = blob;
                    this.objectUrl.set(URL.createObjectURL(blob));
                    this.loading.set(false);
                },
                error: (err) => {
                    this.loading.set(false);
                    this.failed.set(true);
                    this.toast.error(errorMessageFromHttp(err, this.translate));
                },
            });

        // Registered here rather than in ngOnDestroy so the revoke sits beside the create.
        this.destroyRef.onDestroy(() => {
            const url = this.objectUrl();
            if (url) {
                URL.revokeObjectURL(url);
            }
        });
    }

    /** Saves the copy already in memory rather than asking the server for it twice. */
    protected download(): void {
        if (this.blob) {
            BaseService.downloadFile(this.blob, this.data.name);
            return;
        }
        this.examFiles.download(this.data.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: ({blob, filename}) => BaseService.downloadFile(blob, filename ?? this.data.name),
                error: (err) => this.toast.error(errorMessageFromHttp(err, this.translate)),
            });
    }

    /**
     * Public because DialogService calls it from outside when the backdrop or Escape is used.
     * Nothing here can be edited, so it closes straight away rather than asking about unsaved work.
     */
    onCancel(): void {
        this.dialogRef.close();
    }

    protected close(): void {
        this.dialogRef.close();
    }
}

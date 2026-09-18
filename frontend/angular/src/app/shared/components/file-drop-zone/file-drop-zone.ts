import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    input,
    output,
    signal,
    viewChild,
} from '@angular/core';
import {MatIcon} from '@angular/material/icon';
import {TranslatePipe} from '@ngx-translate/core';
import {Button} from '../button/button';

export type FileRejection = {
    readonly file: File;
    readonly reason: 'type' | 'size';
};

/**
 * A drop target with a file picker behind it.
 *
 * Knows nothing about what the files are for — it emits `File[]` and lets the caller decide. Kept
 * here rather than beside its first consumer because the same control is wanted wherever an import
 * lands, and because the drag bookkeeping below is the part worth testing in isolation.
 */
@Component({
    selector: 'app-file-drop-zone',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [Button, MatIcon, TranslatePipe],
    templateUrl: './file-drop-zone.html',
    styleUrl: './file-drop-zone.scss',
    host: {
        'class': 'file-drop-zone',
        'role': 'group',
        '[attr.aria-labelledby]': 'testId() + "-title"',
        '[attr.aria-describedby]': 'testId() + "-hint"',
        '[attr.aria-busy]': 'busy()',
        '[attr.data-testid]': 'testId()',
        '[class.is-dragging]': 'isDragging()',
        '[class.is-busy]': 'busy()',
        '[class.is-disabled]': 'disabled()',
        '[class.has-error]': 'rejections().length > 0',
        '(dragenter)': 'onDragEnter($event)',
        '(dragover)': 'onDragOver($event)',
        '(dragleave)': 'onDragLeave($event)',
        '(drop)': 'onDrop($event)',
        // Convenience for the mouse only. Deliberately not paired with a keydown handler: the
        // button inside is already the keyboard path, and making the whole zone focusable too would
        // give screen-reader users two stops for one action.
        '(click)': 'openPicker()',
    },
})
export class FileDropZone {

    /** Native `accept` attribute value. */
    readonly accept = input<string>('');
    readonly multiple = input<boolean>(true);
    /** Something is uploading; the zone stops accepting input without looking broken. */
    readonly busy = input<boolean>(false);
    readonly disabled = input<boolean>(false);
    readonly maxSizeBytes = input<number>(20 * 1024 * 1024);
    /** A second opinion beyond `accept`, which browsers treat as advice. */
    readonly isAccepted = input<(file: File) => boolean>(() => true);

    readonly titleKey = input<string>('global.dropzone.title');
    readonly hintKey = input<string>('global.dropzone.hint');
    readonly buttonLabelKey = input<string>('global.dropzone.button');
    readonly testId = input<string>('file-drop-zone');

    readonly filesSelected = output<readonly File[]>();
    readonly filesRejected = output<readonly FileRejection[]>();

    private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

    protected readonly isDragging = signal(false);
    protected readonly rejections = signal<readonly FileRejection[]>([]);

    /**
     * `dragleave` fires every time the pointer crosses into a child element, so a plain boolean
     * flickers off the moment the cursor passes over the icon or the button. Counting enter and
     * leave pairs and only clearing at zero is what keeps the highlight steady.
     */
    private dragDepth = 0;

    protected onDragEnter(event: DragEvent): void {
        if (!this.canAcceptDrag(event)) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        this.dragDepth++;
        this.isDragging.set(true);
    }

    /**
     * The `preventDefault` here is load-bearing, not tidiness: the browser's default action for
     * `dragover` is to refuse the drop, so without it `drop` never fires and the browser navigates
     * away to the dragged file instead.
     */
    protected onDragOver(event: DragEvent): void {
        if (!this.canAcceptDrag(event)) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'copy';
        }
    }

    protected onDragLeave(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.dragDepth = Math.max(0, this.dragDepth - 1);
        if (this.dragDepth === 0) {
            this.isDragging.set(false);
        }
    }

    protected onDrop(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
        // Reset rather than decrement: a drop ends the drag outright, however many enters were
        // counted on the way in.
        this.dragDepth = 0;
        this.isDragging.set(false);

        if (this.disabled() || this.busy()) {
            return;
        }
        this.intake(event.dataTransfer?.files ?? null);
    }

    protected openPicker(): void {
        if (this.disabled() || this.busy()) {
            return;
        }
        this.fileInput().nativeElement.click();
    }

    protected onInputChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.intake(input.files);
        // Choosing the same file twice in a row leaves `value` untouched, so `change` never fires
        // the second time. Clearing it makes a re-pick work.
        input.value = '';
    }

    /** Light up for an actual file drag only — not for dragged text, a link or a selection. */
    private canAcceptDrag(event: DragEvent): boolean {
        if (this.disabled() || this.busy()) {
            return false;
        }
        return Array.from(event.dataTransfer?.types ?? []).includes('Files');
    }

    private intake(list: FileList | null): void {
        const all = Array.from(list ?? []);
        if (!all.length) {
            return;
        }

        const candidates = this.multiple() ? all : all.slice(0, 1);
        const accepted: File[] = [];
        const rejected: FileRejection[] = [];
        const check = this.isAccepted();

        for (const file of candidates) {
            // A dropped folder arrives as a File with an empty type and no extension, so the type
            // check turns it away without any directory-entry handling.
            if (!check(file)) {
                rejected.push({file, reason: 'type'});
            } else if (file.size > this.maxSizeBytes()) {
                rejected.push({file, reason: 'size'});
            } else {
                accepted.push(file);
            }
        }

        this.rejections.set(rejected);

        if (accepted.length) {
            this.filesSelected.emit(accepted);
        }
        if (rejected.length) {
            this.filesRejected.emit(rejected);
        }
    }
}

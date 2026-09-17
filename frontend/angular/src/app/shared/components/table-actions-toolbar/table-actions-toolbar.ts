import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewEncapsulation
} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {debounceTime, merge, Subject, Subscription} from 'rxjs';
import {distinctUntilChanged, map} from 'rxjs/operators';

/**
 * Search field for a list screen, plus a slot for the page's own toolbar actions.
 *
 * `label` and `placeholder` are already-translated text, not lexicon keys — the caller owns the
 * wording.
 */
@Component({
    selector: 'app-table-actions-toolbar',
    encapsulation: ViewEncapsulation.None,
    standalone: true,
    imports: [
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatButtonModule,
        ReactiveFormsModule,
    ],
    templateUrl: './table-actions-toolbar.html',
    styleUrl: './table-actions-toolbar.scss',
})
export class TableActionsToolbar implements OnChanges, OnDestroy {
    @Input() label = '';
    @Input() placeholder = '';
    @Input() disabled = false;
    @Input() debounceMs = 500;
    /** Hides the action slot entirely, for a table with neither export nor import. */
    @Input() hideActions = false;

    @Input() set value(v: string) {
        this.ctrl.setValue(v ?? '', {emitEvent: false});
    }

    @Output() valueChange = new EventEmitter<string>();
    @Output() cleared = new EventEmitter<void>();

    ctrl = new FormControl<string>('', {nonNullable: true});

    get hasValue(): boolean {
        return (this.ctrl.value ?? '').trim().length > 0;
    }

    // Clear emits immediately without debounce, so the same search term can be used again after reset.
    private clear$ = new Subject<string>();

    private sub: Subscription = merge(
        this.ctrl.valueChanges.pipe(
            map((v) => (v ?? '').trim()),
            debounceTime(this.debounceMs),
        ),
        this.clear$,
    )
        .pipe(distinctUntilChanged())
        .subscribe((v) => this.valueChange.emit(v));

    clear() {
        // Don't emit manually here; clear$ already pushes the reset value through the shared stream,
        // which keeps distinctUntilChanged in sync and avoids duplicate clear events.
        this.ctrl.setValue('', {emitEvent: false});
        this.clear$.next('');
        this.cleared.emit();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (!('disabled' in changes)) {
            return;
        }

        if (this.disabled) {
            this.ctrl.disable({emitEvent: false});
            return;
        }

        this.ctrl.enable({emitEvent: false});
    }

    ngOnDestroy() {
        this.sub.unsubscribe();
        this.clear$.complete();
    }
}

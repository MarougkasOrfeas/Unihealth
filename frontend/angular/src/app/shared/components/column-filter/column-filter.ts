import {CommonModule} from '@angular/common';
import {Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatMenuModule, MatMenuTrigger} from '@angular/material/menu';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {TranslatePipe} from '@ngx-translate/core';
import {debounceTime, Subject, Subscription} from 'rxjs';
import {Button} from '../button/button';

@Component({
    selector: 'app-column-filter',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatMenuModule,
        MatIconModule,
        MatButtonModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatInputModule,
        Button,
        TranslatePipe,
        MatProgressSpinnerModule,
    ],
    templateUrl: './column-filter.html',
    styleUrl: './column-filter.scss'
})
export class ColumnFilterComponent implements OnInit, OnDestroy {
    private readonly identityDisplay = (value: string) => value;
    private readonly serverSearchChanges = new Subject<string>();
    private readonly subscriptions = new Subscription();
    /**
     * Last server-side search value emitted to the parent component.
     * Used to suppress duplicate searches while still supporting the reopen and
     * repaste flow.
     */
    private lastEmittedSearchValue: string | null = null;

    /** Internal list of available filter options */
    private _options: readonly string[] = [];

    /** Maps raw option values to a user-facing label. */
    private _displayWith: (value: string) => string = (value: string) => value;

    @Input()
    set displayWith(fn: ((value: string) => string) | null | undefined) {
        this._displayWith = fn ?? this.identityDisplay;
    }

    get displayWith(): (value: string) => string {
        return this._displayWith;
    }

    /** Enables or disables text search */
    @Input() searchable = true;

    @Input() serverSearch = false;

    @Input() disabled = false;

    @Input() forceActive = false;

    @Input() customClear = false;

    @Input() loading = false;

    @Input() oneElementSelected = false;

    /**
     * Current selected values provided by the parent component.
     * Updates the internal selection state.
     */
    @Input() set selection(values: Iterable<string> | null | undefined) {
        this.selected = new Set(values ?? []);
        // Recalculate visible options
        this.recompute();
    }

    /**
     * List of available filter options.
     * Triggers recalculation when updated.
     */
    @Input() set options(value: readonly string[] | null | undefined) {
        this._options = value ?? [];
        // Recalculate visible options
        this.recompute();
    }

    /**
     * List of available filter options.
     * Triggers recalculation when updated.
     */
    get options(): readonly string[] {
        return this._options;
    }

    /** Emits the selected values whenever they change */
    @Output() selectionChange = new EventEmitter<Set<string>>();

    /** Emits when the filter menu is opened */
    @Output() opened = new EventEmitter<void>();

    @Output() searchChange = new EventEmitter<string>();

    @Output() clearRequested = new EventEmitter<void>();

    /** Reference to the material menu trigger */
    @ViewChild(MatMenuTrigger) trigger!: MatMenuTrigger;

    /** Reference to the search input element */
    @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

    /** Form control for the search input */
    searchCtrl = new FormControl<string>('', {nonNullable: true});

    /** Set of currently selected options */
    selected = new Set<string>();

    /** Options visible after applying search filter */
    filteredOptions: string[] = [];

    ngOnInit() {
        this.subscriptions.add(
            this.searchCtrl.valueChanges.subscribe((value) => {
                this.recompute();
                if (this.serverSearch) {
                    this.serverSearchChanges.next(value.trim());
                }
            }),
        );

        this.subscriptions.add(
            this.serverSearchChanges.pipe(debounceTime(500)).subscribe((value) => {
                this.emitSearchChange(value);
            }),
        );
    }

    ngOnDestroy() {
        this.subscriptions.unsubscribe();
    }

    /**
     * Adds or removes a value from the selection.
     *
     * @param value The option value
     * @param checked Whether the checkbox is checked
     */
    toggle(value: string, checked: boolean) {
        // Add value if checked, otherwise remove it
        if (checked) {
            if (this.oneElementSelected) {
                this.selected.clear();
            }
            this.selected.add(value);
        } else this.selected.delete(value);

        // Notify parent about selection change
        this.selectionChange.emit(new Set(this.selected));
    }

    /**
     * Clears all selected filters and resets the search field.
     */
    clear() {
        this.searchCtrl.setValue('', {emitEvent: true});
        this.emitSearchChange(this.searchCtrl.value.trim());

        if (this.customClear) {
            this.clearRequested.emit();
            this.searchInput?.nativeElement.focus();
            return;
        }

        // Remove all selected values
        this.selected.clear();
        // Notify parent that selection is cleared
        this.selectionChange.emit(new Set(this.selected));
        // Focus search input
        this.searchInput?.nativeElement.focus();
    }

    /**
     * Selects all currently visible options.
     */
    selectAll() {
        this.selected = new Set([
            ...this.selected,
            ...this.filteredOptions,
        ]);

        this.selectionChange.emit(new Set(this.selected));
        this.searchInput?.nativeElement.focus();
    }

    /**
     * Called when the filter menu is opened.
     * Focuses the search input field.
     */
    onMenuOpened(): void {
        // clear search facet text when opening the menu
        const hadFacetSearchText = !!this.searchCtrl.value;
        if (hadFacetSearchText) {
            this.searchCtrl.setValue('', {emitEvent: false});
            this.recompute();
        }

        if (this.serverSearch && hadFacetSearchText) {
            this.emitSearchChange('');
            this.searchInput?.nativeElement.focus();
            return; // do not call again loadFacetOptions from open.emit->onFacetOpened->searchChange
        }

        // Focus search input
        this.searchInput?.nativeElement.focus();
        // Notify parent that menu was opened
        this.opened.emit();
    }

    /**
     * Updates the list of visible options based on search text.
     */
    private recompute() {
        const q = this.searchCtrl.value.trim().toLowerCase();
        const selectedOptions = Array.from(this.selected);
        const optionsWithoutSelected = this.options.filter((option) => !this.selected.has(option));
        const rest = this.options
            .filter((option) => !this.selected.has(option))
            .filter((option) => this.serverSearch || !this.searchable || !q || this.displayWith(option).toLowerCase().includes(q));

        const missingSelected = selectedOptions.filter((option) => !this.options.includes(option));
        const visibleSelected = [...this.options.filter((option) => this.selected.has(option)), ...missingSelected];
        const visibleRest = this.serverSearch ? optionsWithoutSelected : rest;

        this.filteredOptions = [...visibleSelected, ...visibleRest];
    }

    /**
     * Emits a server-side facet search term only when it differs from the last
     * emitted value.
     *
     * This method replaces `distinctUntilChanged()` on the debounced stream
     * because `distinctUntilChanged()` cannot be reset when reopening the facet
     * clears the input without emitting a form-control value change. Without this
     * explicit tracking, pasting a value, closing the facet, reopening it, and
     * pasting the same value again would be ignored as a duplicate.
     */
    private emitSearchChange(value: string): void {
        if (value === this.lastEmittedSearchValue) {
            return;
        }

        this.lastEmittedSearchValue = value;
        this.searchChange.emit(value);
    }

}

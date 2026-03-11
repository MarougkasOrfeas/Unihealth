import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatMenuTrigger, MatMenuModule} from '@angular/material/menu';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {CommonModule} from '@angular/common';

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
  ],
  templateUrl: './column-filter.html',
  styleUrl: './column-filter.scss'
})
export class ColumnFilterComponent implements OnInit {

  /** Internal list of available filter options */
  private _options: string[] = [];

  /** Enables or disables text search */
  @Input() searchable = true;

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
  @Input() set options(value: string[] | null | undefined) {
    this._options = value ?? [];
    // Recalculate visible options
    this.recompute();
  }

  /**
   * List of available filter options.
   * Triggers recalculation when updated.
   */
  get options(): string[] {
    return this._options;
  }

  /** Emits the selected values whenever they change */
  @Output() selectionChange = new EventEmitter<Set<string>>();

  /** Emits when the filter menu is opened */
  @Output() opened = new EventEmitter<void>();

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
    // // Listen for search text changes
    this.searchCtrl.valueChanges.subscribe(() => this.recompute());
  }

  /**
   * Adds or removes a value from the selection.
   *
   * @param value The option value
   * @param checked Whether the checkbox is checked
   */
  toggle(value: string, checked: boolean) {
    // Add value if checked, otherwise remove it
    if (checked) this.selected.add(value);
    else this.selected.delete(value);

    // Notify parent about selection change
    this.selectionChange.emit(new Set(this.selected));
  }

  /**
   * Clears all selected filters and resets the search field.
   */
  clear() {
    // Remove all selected values
    this.selected.clear();
    // Notify parent that selection is cleared
    this.selectionChange.emit(new Set(this.selected));
    // clear search input
    this.searchCtrl.setValue('', {emitEvent: true});
    // Focus search input
    this.searchInput?.nativeElement.focus();
  }

  /**
   * Selects all available options.
   */
  selectAll() {
    // Mark all options as selected
    this.selected = new Set(this.options);
    // Notify parent about selection change
    this.selectionChange.emit(new Set(this.selected));
  }

  /**
   * Called when the filter menu is opened.
   * Focuses the search input field.
   */
  onMenuOpened() {
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

    // Always show selected filters (even if they don't match search)
    const selectedFirst = this.options.filter(o => this.selected.has(o));

    // Filter only the NOT-selected items by the search text
    const rest = this.options
      .filter(o => !this.selected.has(o))
      .filter(o => !this.searchable || !q || o.toLowerCase().includes(q));

    // Pin Selected items on top
    this.filteredOptions = [...selectedFirst, ...rest];
  }

}

import {ChangeDetectionStrategy, Component, computed, input, linkedSignal, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatSortModule, Sort} from '@angular/material/sort';
import {MatTableModule} from '@angular/material/table';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ColumnFilterComponent} from '../column-filter/column-filter';
import {MockTableColumn} from './mock-table-column';

/**
 * Table for screens backed by in-memory mock data, with no backend behind them.
 *
 * Everything runs in the browser: the facet options are derived from the rows, and filtering,
 * sorting and paging are applied here. Hand it `columns` and `data` and it owns the rest — the
 * caller keeps no filter or page state.
 *
 * This is deliberately *not* {@link BaseTable}, which is server-driven and requires a
 * `BaseService` to page, facet and export against. When a screen using this one gets a real
 * backend, it should move to `BaseTable` rather than this component growing a server mode.
 */
@Component({
    selector: 'app-mock-table',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        MatTableModule,
        MatSortModule,
        MatPaginatorModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        DatePipe,
        ColumnFilterComponent,
    ],
    templateUrl: './mock-table.html',
    styleUrls: ['./mock-table.scss'],
})
export class MockTable<T extends Record<string, any>> {

    readonly columns = input<readonly MockTableColumn<T>[]>([]);
    readonly data = input<readonly T[]>([]);
    readonly pageSize = input(10);
    readonly pageSizeOptions = input<readonly number[]>([10, 20, 50]);
    readonly loading = input(false);
    readonly emptyTitle = input('Δεν βρέθηκαν αποτελέσματα');
    readonly emptySubtitle = input('Δοκιμάστε να αλλάξετε τα φίλτρα.');

    /** Column key -> selected raw cell values. Owned here, not by the page. */
    private readonly facetSelection = signal<Readonly<Record<string, ReadonlySet<string>>>>({});
    private readonly sort = signal<Sort>({active: '', direction: ''});
    private readonly pageIndex = signal(0);
    /** Follows the `pageSize` input until the user picks a different size. */
    protected readonly currentPageSize = linkedSignal(() => this.pageSize());

    /** A stable empty set, so `column-filter`'s selection setter is not handed a new object per
     *  change-detection pass. */
    protected readonly noSelection: ReadonlySet<string> = new Set<string>();

    protected readonly displayedColumns = computed(
        () => this.columns().map((column) => String(column.key)),
    );

    private readonly filterableColumns = computed(
        () => this.columns().filter((column) => column.filterable),
    );

    /** Rows passing every active facet. */
    protected readonly filteredRows = computed(
        () => this.data().filter((row) => this.matchesFacets(row)),
    );

    protected readonly totalElements = computed(() => this.filteredRows().length);

    private readonly sortedRows = computed(() => {
        const {active, direction} = this.sort();
        const rows = this.filteredRows();
        if (!active || !direction) {
            return rows;
        }

        const column = this.columns().find((c) => String(c.key) === active);
        if (!column) {
            return rows;
        }

        const factor = direction === 'asc' ? 1 : -1;
        // Copy first: the computed must not reorder the array the `data` input still points at.
        return [...rows].sort((a, b) =>
            factor * this.cellValue(a, column)
                .localeCompare(this.cellValue(b, column), undefined, {numeric: true}),
        );
    });

    /**
     * Clamped rather than reset: filtering down to fewer pages while sitting on a late page should
     * land on the last page that still has rows, not silently show an empty table.
     */
    protected readonly currentPageIndex = computed(() => {
        const lastPage = Math.max(0, Math.ceil(this.totalElements() / this.currentPageSize()) - 1);
        return Math.min(this.pageIndex(), lastPage);
    });

    protected readonly pagedRows = computed(() => {
        const start = this.currentPageIndex() * this.currentPageSize();
        return this.sortedRows().slice(start, start + this.currentPageSize());
    });

    /**
     * Facet options per filterable column, each derived from the rows that pass *the other*
     * facets. Narrowing one column therefore narrows what the others offer, so a combination that
     * would return nothing cannot be selected.
     */
    protected readonly facetOptions = computed<Readonly<Record<string, readonly string[]>>>(() => {
        const options: Record<string, string[]> = {};

        for (const column of this.filterableColumns()) {
            const key = String(column.key);
            const scoped = this.data().filter((row) => this.matchesFacets(row, key));
            const values = new Set<string>();

            for (const row of scoped) {
                for (const value of this.facetValues(row, column)) {
                    values.add(value);
                }
            }

            // Keep already-selected values visible even when the other facets exclude them, so a
            // selection can always be undone from the dropdown that made it.
            for (const selected of this.facetSelection()[key] ?? []) {
                values.add(selected);
            }

            options[key] = [...values].sort((a, b) => a.localeCompare(b, undefined, {numeric: true}));
        }

        return options;
    });

    protected getFacetOptions(key: string): readonly string[] {
        return this.facetOptions()[key] ?? [];
    }

    protected getFacetSelection(key: string): ReadonlySet<string> {
        return this.facetSelection()[key] ?? this.noSelection;
    }

    protected onFacetChange(key: string, selection: ReadonlySet<string>): void {
        this.facetSelection.update((current) => ({...current, [key]: new Set(selection)}));
        this.pageIndex.set(0);
    }

    protected onSortChanged(sort: Sort): void {
        this.sort.set(sort);
        this.pageIndex.set(0);
    }

    protected onPageChanged(event: PageEvent): void {
        this.pageIndex.set(event.pageIndex);
        this.currentPageSize.set(event.pageSize);
    }

    protected getCellValue(row: T, column: MockTableColumn<T>): unknown {
        return column.valueFn ? column.valueFn(row) : row[column.key as keyof T];
    }

    /** The cell as comparable text, for sorting. */
    private cellValue(row: T, column: MockTableColumn<T>): string {
        const value = this.getCellValue(row, column);
        return value === null || value === undefined ? '' : String(value);
    }

    /** The values this row contributes to (and is matched against for) a facet. */
    private facetValues(row: T, column: MockTableColumn<T>): string[] {
        if (column.facetValues) {
            return column.facetValues(row).filter(Boolean);
        }
        const value = this.cellValue(row, column);
        return value ? [value] : [];
    }

    /** Whether a row passes every active facet, optionally ignoring one column. */
    private matchesFacets(row: T, exceptKey?: string): boolean {
        const selection = this.facetSelection();

        for (const column of this.filterableColumns()) {
            const key = String(column.key);
            if (key === exceptKey) {
                continue;
            }

            const selected = selection[key];
            if (!selected?.size) {
                continue;
            }

            if (!this.facetValues(row, column).some((value) => selected.has(value))) {
                return false;
            }
        }

        return true;
    }
}

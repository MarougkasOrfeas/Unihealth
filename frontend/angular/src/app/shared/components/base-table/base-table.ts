import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    OnInit,
    output,
    signal,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {DatePipe} from '@angular/common';
import {MatIcon} from '@angular/material/icon';
import {MatIconButton} from '@angular/material/button';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {MatTableModule} from '@angular/material/table';
import {MatTooltip} from '@angular/material/tooltip';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {Subject, switchMap} from 'rxjs';
import {PermissionService} from '../../../core/auth/permission.service';
import {OverflowTooltipDirective} from '../../directives/overflow-tooltip.directive';
import {BaseEntity, FacetPredicateParams} from '../../interfaces/baseEntity';
import {BaseService} from '../../services/base.service';
import {SearchStateService} from '../../services/search-state.service';
import {ToastService} from '../../services/toast.service';
import {errorMessageFromHttp} from '../../utils/http-error.util';
import {ActiveFilter, ActiveFiltersToolbar} from '../active-filters-toolbar/active-filters-toolbar';
import {Button} from '../button/button';
import {Chip} from '../chip/chip';
import {ColumnFilterComponent} from '../column-filter/column-filter';
import {ImportExportActionsMenuComponent} from '../import-export-actions-menu/import-export-actions-menu';
import {MultisortSection, SortChip} from '../multisort-section/multisort-section';
import {SortIndicator} from '../multisort-section/table-sort-indicator/table-sort-indicator';
import {NoDataTable} from '../no-data-table/no-data-table';
import {SpinnerComponent} from '../spinner/spinner';
import {TableActionsToolbar} from '../table-actions-toolbar/table-actions-toolbar';
import {ViewMoreListComponent} from '../view-more-list/view-more-list';

/** Key of the trailing, sticky actions column. Owned by the table, never declared by a page. */
export const ACTIONS_COLUMN = 'actions';

export type SortDir = 'asc' | 'desc';

export interface MultiSortEntry {
    readonly key: string;
    readonly dir: SortDir;
}

export type BaseTableRow = { readonly id: string } & Record<string, unknown>;

export type CellType = 'text' | 'date' | 'status' | 'list' | 'check';

export type StatusTone = 'active' | 'inactive' | 'unverified';

export type RowAction = 'view' | 'edit' | 'delete';

export type EmptyStateData = {
    titleKey: string;
    subtitleKey: string;
    titleErrorKey: string;
    subtitleErrorKey: string;
    buttonLabelKey?: string;
};

export type TableColumnData<TRow extends BaseTableRow> = {
    /** Row property this column reads; also its sort and facet identity on the UI side. */
    key: string & keyof TRow;
    /** Lexicon key of the header label. */
    label: string;
    /**
     * Backend entity path used for sorting, filtering and facet options, e.g.
     * `'department.group.name'`. Defaults to {@link key}.
     *
     * Living on the column removes the need for a parallel UI-to-backend dictionary, which could
     * silently drift out of sync with the column list.
     */
    field?: string;

    sortable?: boolean;
    /** Send ascending inverted, so an "Active" column lists active rows first. */
    sortDescFirst?: boolean;
    filterable?: boolean;

    /** How the cell renders. Defaults to `'text'`. */
    cellType?: CellType;
    /** Cell text for `'text'` and `'check'`. Defaults to `String(row[key])`. */
    display?: (row: TRow) => string;
    /** Required when {@link cellType} is `'status'`. `labelKey` is a lexicon key. */
    status?: (row: TRow) => { tone: StatusTone; labelKey: string };
    /** `'date'` only. */
    withTime?: boolean;
    /** `'list'` only. */
    listTitle?: string;
    listChips?: boolean;
    emptyChipLabel?: string;

    /** Truncate with an ellipsis and show the full value as a tooltip when it overflows. */
    truncate?: boolean;
    tooltip?: (row: TRow) => string;
    /** Lexicon key of a header tooltip. */
    headerTooltip?: string;
    maxWidth?: string;

    /**
     * Raw backend facet value to the lexicon key (or literal) shown in the dropdown.
     *
     * Needed because `_facet` answers with the stringified column value — a boolean column
     * answers `'true'`/`'false'` and an enum answers `'ACTIVE'`, neither of which is showable.
     */
    facetLabel?: (value: string) => string;
    /** Selected raw values to whatever goes on {@link field} in the request body. */
    facetToBackend?: (values: string[]) => unknown[];
    /** Fixed option list. When set, opening the dropdown never calls `_facet`. */
    facetOptions?: readonly string[];
    /** Defaults to `true`. Turn off for a facet with only a handful of values. */
    facetSearchable?: boolean;
    facetSingleSelect?: boolean;
};

/**
 * Server-driven table for the administration list screens: search, faceted column filters,
 * multi-sort, pagination, Excel export, an import hook and row actions.
 *
 * It owns the query and fetches its own data, so a page supplies a service, a column list and a
 * row mapper. The service arrives as a typed input rather than through an injection token, which
 * keeps the DTO type all the way through to {@link mapToRow} and lets a test pass a plain fake.
 *
 * Selection is never held here; rows are acted on one at a time through the action column.
 */
@Component({
    selector: 'app-base-table',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ActiveFiltersToolbar,
        Button,
        Chip,
        ColumnFilterComponent,
        DatePipe,
        ImportExportActionsMenuComponent,
        MatIcon,
        MatIconButton,
        MatPaginator,
        MatTableModule,
        MatTooltip,
        MultisortSection,
        NoDataTable,
        OverflowTooltipDirective,
        SortIndicator,
        SpinnerComponent,
        TableActionsToolbar,
        TranslatePipe,
        ViewMoreListComponent,
    ],
    templateUrl: './base-table.html',
    styleUrl: './base-table.scss',
})
export class BaseTable<TDto extends BaseEntity, TRow extends BaseTableRow> implements OnInit {

    private readonly translate = inject(TranslateService);
    private readonly toast = inject(ToastService);
    private readonly searchState = inject(SearchStateService);
    private readonly permissions = inject(PermissionService);
    private readonly destroyRef = inject(DestroyRef);

    // ─── Required inputs ───

    readonly service = input.required<BaseService<TDto>>();
    readonly columns = input.required<readonly TableColumnData<TRow>[]>();
    readonly mapToRow = input.required<(dto: TDto) => TRow>();
    /** Identifies the list for state restore, and prefixes every `data-testid`. */
    readonly pageName = input.required<string>();

    // ─── Optional inputs ───

    readonly emptyState = input<EmptyStateData>({
        titleKey: 'global.list.noResults.title',
        subtitleKey: 'global.list.noResults.subtitle',
        titleErrorKey: 'global.list.error.title',
        subtitleErrorKey: 'global.list.error.subtitle',
    });
    /** Sort applied when the user has not chosen one, e.g. `['name,asc']`. */
    readonly defaultSort = input<readonly string[]>([]);
    readonly pageSize = input<number>(10);
    readonly searchLabel = input<string>('global.search');
    readonly rowActions = input<readonly RowAction[]>(['edit', 'delete']);
    readonly showExport = input<boolean>(false);
    readonly showImport = input<boolean>(false);
    /**
     * Shows the Import item but blocks it. There is no import endpoint on the backend yet, so the
     * pages surface the action as coming-soon rather than hiding it and letting it look missing.
     */
    readonly importDisabled = input<boolean>(false);
    readonly isImporting = input<boolean>(false);
    /** Offer the create button in the empty state as well as the page header. */
    readonly emptyStateCreate = input<boolean>(false);
    readonly stickyHeader = input<boolean>(true);
    /**
     * Overrides the admin check for pages with their own rule. Left `undefined` it follows
     * {@link PermissionService.isAdmin}; it cannot be an `input(false)` default, because an input
     * default is evaluated once at construction and would freeze while the rights request is in
     * flight.
     */
    readonly canManage = input<boolean | undefined>(undefined);

    // ─── Outputs ───

    readonly create = output<void>();
    readonly view = output<TRow>();
    readonly edit = output<TRow>();
    readonly delete = output<TRow>();
    readonly importFileSelected = output<Event>();

    // ─── Query state ───

    protected readonly rows = signal<readonly TRow[]>([]);
    protected readonly total = signal(0);
    protected readonly isLoading = signal(true);
    protected readonly loadError = signal(false);
    protected readonly isExporting = signal(false);
    protected readonly searchValue = signal('');
    protected readonly pageIndex = signal(0);
    protected readonly currentPageSize = signal(10);
    protected readonly multiSort = signal<readonly MultiSortEntry[]>([]);
    // `| undefined` on the value type because `noUncheckedIndexedAccess` is off, and without it the
    // template's `?? []` fallbacks read as redundant to the compiler.
    protected readonly facetOptions =
        signal<Readonly<Record<string, readonly string[] | undefined>>>({});
    protected readonly facetSelection =
        signal<Readonly<Record<string, ReadonlySet<string> | undefined>>>({});
    protected readonly facetLoading = signal<ReadonlySet<string>>(new Set());
    private readonly facetSearchTerms = new Map<string, string>();

    /** View-only state, so it lives in the view and never invalidates a query computed. */
    protected hoveredSortColumn = signal<string | null>(null);

    private readonly reload$ = new Subject<void>();

    /** Handed to `column-filter` for columns with no selection; a fresh `Set` per change-detection
     *  pass would make its `selection` setter recompute on every cycle. */
    protected readonly noSelection: ReadonlySet<string> = new Set<string>();

    private static readonly MAX_SORT_COLUMNS = 3;
    private static readonly LARGE_EXPORT_THRESHOLD = 100;

    // ─── Derived view state ───

    protected readonly manageAllowed = computed(
        () => this.canManage() ?? this.permissions.isAdmin(),
    );

    protected readonly hasActions = computed(() => this.rowActions().length > 0);

    protected readonly displayedColumns = computed(() => {
        const keys = this.columns().map((column) => column.key as string);
        return this.hasActions() ? [...keys, ACTIONS_COLUMN] : keys;
    });

    protected readonly filterableColumns = computed(
        () => this.columns().filter((column) => column.filterable),
    );

    protected readonly sortOrder = computed(() => {
        const order: Record<string, { index: number; dir: SortDir }> = {};
        this.multiSort().forEach((entry, i) => (order[entry.key] = {index: i + 1, dir: entry.dir}));
        return order;
    });

    /** Chips carry lexicon keys; the template translates them, so a language switch is picked up
     *  by the impure `TranslatePipe` without this computed having to depend on the locale. */
    protected readonly sortChips = computed<SortChip[]>(() =>
        this.multiSort().map((entry) => ({
            key: entry.key,
            label: this.column(entry.key)?.label ?? entry.key,
            dirLabel: entry.dir === 'asc' ? 'global.sort.asc' : 'global.sort.desc',
        })),
    );

    protected readonly activeFilters = computed<ActiveFilter[]>(() => {
        const selection = this.facetSelection();
        return this.filterableColumns()
            .filter((column) => (selection[column.key]?.size ?? 0) > 0)
            .map((column) => ({
                key: column.key as string,
                label: column.label,
                values: Array.from(selection[column.key] ?? []),
            }));
    });

    protected readonly exportDisabled = computed(
        () => this.isLoading() || this.isExporting() || this.total() === 0,
    );

    /** A search term or facet is active, so "no results" is the honest empty state rather than
     *  "nothing here yet". */
    protected readonly isFiltered = computed(
        () => !!this.searchValue().trim() || this.activeFilters().length > 0,
    );

    protected readonly emptyStateTitleKey = computed(() => {
        if (this.loadError()) {
            return this.emptyState().titleErrorKey;
        }
        return this.isFiltered() ? 'global.list.noResults.title' : this.emptyState().titleKey;
    });

    protected readonly emptyStateSubtitleKey = computed(() => {
        if (this.loadError()) {
            return this.emptyState().subtitleErrorKey;
        }
        return this.isFiltered() ? 'global.list.noResults.subtitle' : this.emptyState().subtitleKey;
    });

    protected readonly ACTIONS_COLUMN = ACTIONS_COLUMN;

    // ─── Lifecycle ───

    ngOnInit(): void {
        this.currentPageSize.set(this.pageSize());
        this.restoreState();

        this.reload$
            .pipe(
                // switchMap, not a bare subscribe: a facet change fires a page load and a facet
                // load together, and without cancellation a stale page response can land last.
                switchMap(() => this.service().getPage(this.buildRequestBody())),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: (page) => {
                    const mapper = this.mapToRow();
                    const rows = (page.content ?? []).map((dto) => mapper(dto));
                    const total = page.totalElements ?? rows.length;

                    // Deleting the last row of a page leaves it empty; fall back to the last page
                    // that still has content rather than showing an empty table.
                    const lastPage = total > 0 ? Math.ceil(total / this.currentPageSize()) - 1 : 0;
                    if (this.pageIndex() > lastPage) {
                        this.pageIndex.set(lastPage);
                        this.loadData();
                        return;
                    }

                    this.rows.set(rows);
                    this.total.set(total);
                    this.isLoading.set(false);
                },
                error: (err) => {
                    console.error(`Failed to load ${this.pageName()}`, err);
                    this.rows.set([]);
                    this.total.set(0);
                    this.isLoading.set(false);
                    this.loadError.set(true);
                },
            });

        this.loadData();
    }

    // ─── Data loading ───

    /** Reloads the current page. */
    loadData(): void {
        this.isLoading.set(true);
        this.loadError.set(false);
        this.reload$.next();
    }

    /** Reloads from page one. Use after a create or a delete, which shift every later row. */
    reloadFirstPage(): void {
        this.pageIndex.set(0);
        this.loadData();
    }

    private resetPageAndLoad(): void {
        this.pageIndex.set(0);
        this.loadData();
    }

    private buildRequestBody(): Record<string, unknown> {
        return {
            page: this.pageIndex(),
            size: this.currentPageSize(),
            sort: this.buildSortParameters(),
            ...this.facetFilters(),
            ...this.searchParam(),
        };
    }

    private searchParam(): Record<string, unknown> {
        const search = this.searchValue().trim();
        return search ? {search} : {};
    }

    private buildSortParameters(): string[] {
        const sorts = this.multiSort();
        if (!sorts.length) {
            return [...this.defaultSort()];
        }

        return sorts.map((entry) => {
            const column = this.column(entry.key);
            const field = column?.field ?? entry.key;
            const dir = column?.sortDescFirst
                ? (entry.dir === 'asc' ? 'desc' : 'asc')
                : entry.dir;
            return `${field},${dir}`;
        });
    }

    /** Facet filters for the request body, shared with the `_facet` predicate params. */
    private facetFilters(excludeKey?: string): Record<string, unknown> {
        const selection = this.facetSelection();
        const filters: Record<string, unknown> = {};

        for (const column of this.filterableColumns()) {
            if (column.key === excludeKey) {
                continue;
            }
            const selected = selection[column.key];
            if (!selected?.size) {
                continue;
            }
            const values = Array.from(selected);
            filters[column.field ?? column.key] =
                column.facetToBackend ? column.facetToBackend(values) : values;
        }

        return filters;
    }

    protected column(key: string): TableColumnData<TRow> | undefined {
        return this.columns().find((column) => column.key === key);
    }

    // ─── Search ───

    protected onSearchValueChange(value: string): void {
        this.searchValue.set(value ?? '');
        this.resetPageAndLoad();
    }

    // ─── Pagination ───

    protected onPageChange(event: PageEvent): void {
        this.pageIndex.set(event.pageIndex);
        this.currentPageSize.set(event.pageSize);
        this.loadData();
    }

    // ─── Sorting ───

    protected onHeaderClick(key: string, event: MouseEvent): void {
        if (this.isLoading()) {
            return;
        }
        event.stopPropagation();

        const sorts = this.multiSort();
        const existing = sorts.findIndex((entry) => entry.key === key);

        if (existing >= 0) {
            // asc -> desc -> off
            const next = sorts[existing].dir === 'asc'
                ? sorts.map((entry, i) => (i === existing ? {key, dir: 'desc' as SortDir} : entry))
                : sorts.filter((_, i) => i !== existing);
            this.multiSort.set(next);
        } else {
            const appended = [...sorts, {key, dir: 'asc' as SortDir}];
            // Oldest sort is evicted once the cap is reached, so a click always does something.
            this.multiSort.set(
                appended.length > BaseTable.MAX_SORT_COLUMNS ? appended.slice(1) : appended,
            );
        }

        this.resetPageAndLoad();
    }

    protected removeSort(key: string): void {
        this.multiSort.update((sorts) => sorts.filter((entry) => entry.key !== key));
        this.resetPageAndLoad();
    }

    protected clearAllSorts(): void {
        this.multiSort.set([]);
        this.resetPageAndLoad();
    }

    protected ariaSort(key: string): 'ascending' | 'descending' | 'none' {
        const dir = this.sortOrder()[key]?.dir;
        return dir === 'asc' ? 'ascending' : dir === 'desc' ? 'descending' : 'none';
    }

    // ─── Facets ───

    /**
     * Display labels are produced here rather than being stored, so the selection always holds raw
     * backend values. Switching language then re-labels the chips instead of invalidating them.
     */
    protected facetDisplayFor(column: TableColumnData<TRow>): (value: string) => string {
        return (value: string) =>
            column.facetLabel ? this.translate.instant(column.facetLabel(value)) : value;
    }

    protected readonly facetChipLabel = (key: string, value: string): string => {
        const column = this.column(key);
        return column?.facetLabel ? this.translate.instant(column.facetLabel(value)) : value;
    };

    protected onFacetOpened(key: string): void {
        const column = this.column(key);
        if (!column) {
            return;
        }

        if (column.facetOptions) {
            this.setFacetOptions(key, column.facetOptions);
            return;
        }

        this.loadFacetOptions(column);
    }

    protected onFacetSearchChange(key: string, term: string): void {
        const column = this.column(key);
        if (!column || column.facetOptions) {
            return;
        }

        const trimmed = term.trim();
        if (trimmed) {
            this.facetSearchTerms.set(key, trimmed);
        } else {
            this.facetSearchTerms.delete(key);
        }

        this.loadFacetOptions(column, true);
    }

    protected onFacetChange(key: string, selected: ReadonlySet<string>): void {
        this.facetSelection.update((selection) => ({...selection, [key]: new Set(selected)}));
        this.resetPageAndLoad();
    }

    protected removeFilter(key: string): void {
        this.facetSelection.update((selection) => {
            const next = {...selection};
            delete next[key];
            return next;
        });
        this.resetPageAndLoad();
    }

    protected clearAllFilters(): void {
        this.facetSelection.set({});
        this.resetPageAndLoad();
    }

    private loadFacetOptions(column: TableColumnData<TRow>, excludeSelf = false): void {
        const key = column.key as string;
        const field = column.field ?? key;
        const requestedTerm = this.facetSearchTerms.get(key) ?? '';

        this.setFacetLoading(key, true);

        this.service()
            .getFacetOptions({
                column: field,
                columnFilter: requestedTerm,
                search: this.searchValue().trim(),
                locale: this.translate.getCurrentLang(),
                predicateParams: this.facetFilters(
                    excludeSelf ? key : undefined,
                ) as FacetPredicateParams,
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (options) => {
                    // A slower earlier request must not overwrite the newest search term's result.
                    if ((this.facetSearchTerms.get(key) ?? '') !== requestedTerm) {
                        return;
                    }
                    this.setFacetOptions(key, (options ?? []).map(String));
                    this.setFacetLoading(key, false);
                },
                error: (err) => {
                    this.toast.error(errorMessageFromHttp(err, this.translate));
                    this.setFacetLoading(key, false);
                },
            });
    }

    private setFacetOptions(key: string, options: readonly string[]): void {
        const unique = Array.from(new Set(options));
        this.facetOptions.update((all) => ({...all, [key]: unique}));
    }

    private setFacetLoading(key: string, loading: boolean): void {
        this.facetLoading.update((all) => {
            const next = new Set(all);
            if (loading) {
                next.add(key);
            } else {
                next.delete(key);
            }
            return next;
        });
    }

    // ─── Import / export ───

    protected onExport(): void {
        if (this.exportDisabled()) {
            return;
        }

        this.isExporting.set(true);
        this.toast.info(
            this.translate.instant('global.message.export.progress'),
            this.total() > BaseTable.LARGE_EXPORT_THRESHOLD,
        );

        this.service()
            .getExport({
                page: 0,
                size: this.total(),
                sort: this.buildSortParameters(),
                ...this.facetFilters(),
                ...this.searchParam(),
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (result) => {
                    BaseService.downloadFile(result.blob, result.filename);
                    this.isExporting.set(false);
                    this.toast.success(this.translate.instant('global.message.export.success'));
                },
                error: () => {
                    this.isExporting.set(false);
                    this.toast.error(this.translate.instant('global.message.export.error'));
                },
            });
    }

    protected onImportFileSelected(event: Event): void {
        this.importFileSelected.emit(event);
    }

    // ─── Row actions ───

    protected onCreate(): void {
        this.create.emit();
    }

    protected onView(row: TRow): void {
        this.view.emit(row);
    }

    protected onEdit(row: TRow): void {
        this.edit.emit(row);
    }

    protected onDelete(row: TRow): void {
        this.delete.emit(row);
    }

    // ─── Cell value coercion ───
    // `BaseTableRow` values are `unknown`, which `strictTemplates` will not interpolate or hand to
    // `DatePipe`. These keep the coercion in one place rather than scattered through the template.

    protected cellText(column: TableColumnData<TRow>, row: TRow): string {
        if (column.display) {
            return column.display(row);
        }
        const value = row[column.key];
        return value === null || value === undefined || value === '' ? '-' : String(value);
    }

    protected cellDate(column: TableColumnData<TRow>, row: TRow): Date | string | null {
        const value = row[column.key];
        return value instanceof Date || typeof value === 'string' ? value : null;
    }

    protected cellList(column: TableColumnData<TRow>, row: TRow): readonly string[] {
        const value = row[column.key];
        return Array.isArray(value) ? value.filter((v): v is string => typeof v === 'string') : [];
    }

    // ─── Cross-navigation state ───

    /**
     * Saves the current query so returning from a detail page lands on the same filters, sort and
     * page. Call it immediately before navigating away.
     */
    saveCurrentState(): void {
        const facets: Record<string, string[]> = {};
        for (const [key, values] of Object.entries(this.facetSelection())) {
            if (values?.size) {
                facets[key] = Array.from(values);
            }
        }

        this.searchState.save(this.pageName(), {
            search: this.searchValue(),
            facets,
            sort: [...this.multiSort()],
            pageIndex: this.pageIndex(),
            pageSize: this.currentPageSize(),
        });
    }

    private restoreState(): void {
        // Read-and-remove, so a snapshot is consumed by exactly one restore and re-entering the
        // list from the nav menu starts clean.
        const saved = this.searchState.take(this.pageName());
        if (!saved) {
            return;
        }

        this.searchValue.set(saved.search);
        this.multiSort.set(saved.sort);
        this.pageIndex.set(saved.pageIndex);
        this.currentPageSize.set(saved.pageSize);

        const selection: Record<string, ReadonlySet<string>> = {};
        for (const [key, values] of Object.entries(saved.facets)) {
            selection[key] = new Set(values);
        }
        this.facetSelection.set(selection);
    }
}

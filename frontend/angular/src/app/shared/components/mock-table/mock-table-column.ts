export interface MockTableColumn<T = any> {
    key: keyof T | string;
    /** Already-translated header text. */
    header: string;
    sortable?: boolean;
    type?: 'text' | 'date' | 'status' | 'actions';
    cellClass?: string;
    headerClass?: string;
    /** Derives the displayed value when it is not simply `row[key]`. */
    valueFn?: (row: T) => string;
    actions?: MockTableAction<T>[];

    /** Shows a facet dropdown in the header, filtering the rows held by the table. */
    filterable?: boolean;
    /** Shows the search box inside the facet dropdown. Defaults to `true`. */
    filterSearchable?: boolean;
    /**
     * Splits one cell into several facet values, for a column holding a list in a single string
     * (e.g. `'Επείγοντα, παθολογική κλινική'`). Without it the whole cell value is one option,
     * which for a free-text column means one option per row.
     *
     * A row matches the facet when any of its values is selected.
     */
    facetValues?: (row: T) => string[];
}

export interface MockTableAction<T = any> {
    icon: string;
    tooltip: string;
    disabled?: (row: T) => boolean;
    onClick: (row: T) => void;
}

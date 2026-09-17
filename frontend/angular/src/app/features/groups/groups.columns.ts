import {TableColumnData} from '../../shared/components/base-table/base-table';

/** A type alias, not an interface: `BaseTableRow` requires an index signature, and only a type
 *  alias of an object literal gets one implicitly. */
export type GroupRow = {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly departments: readonly string[];
    readonly active: boolean;
};

/**
 * Columns for the schools list.
 *
 * Kept as plain data in its own file so the page component stays about behaviour, and so the
 * column list can be read (or tested) without instantiating a component.
 */
export const GROUP_COLUMNS: readonly TableColumnData<GroupRow>[] = [
    {
        key: 'name',
        label: 'group.column.name',
        sortable: true,
        filterable: true,
    },
    {
        key: 'description',
        label: 'group.column.description',
        sortable: true,
        filterable: true,
        truncate: true,
        maxWidth: '24rem',
    },
    {
        key: 'departments',
        label: 'group.column.departments',
        // Sorted and filtered on the association path; the backend sorts collections with listagg.
        field: 'departments.name',
        sortable: true,
        filterable: true,
        cellType: 'list',
        listTitle: 'group.column.departments',
        listChips: true,
        emptyChipLabel: 'group.column.departments.empty',
    },
    {
        key: 'active',
        label: 'group.column.active',
        sortable: true,
        // Ascending puts active rows first, which is what a reader expects of a status column.
        sortDescFirst: true,
        filterable: true,
        facetSearchable: false,
        // `_facet` casts the column to text, so a boolean answers 'true'/'false'.
        facetOptions: ['true', 'false'],
        facetLabel: (value) => value === 'true' ? 'global.status.active' : 'global.status.inactive',
        facetToBackend: (values) => values.map((value) => value === 'true'),
        cellType: 'status',
        status: (row) => row.active
            ? {tone: 'active', labelKey: 'global.status.active'}
            : {tone: 'inactive', labelKey: 'global.status.inactive'},
    },
];

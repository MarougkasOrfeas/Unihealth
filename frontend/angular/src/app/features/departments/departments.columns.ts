import {TableColumnData} from '../../shared/components/base-table/base-table';

/** A type alias, not an interface: `BaseTableRow` requires an index signature, and only a type
 *  alias of an object literal gets one implicitly. */
export type DepartmentRow = {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly group: string;
    readonly users: readonly string[];
    readonly active: boolean;
};

/** Columns for the departments list. */
export const DEPARTMENT_COLUMNS: readonly TableColumnData<DepartmentRow>[] = [
    {
        key: 'name',
        label: 'department.column.name',
        sortable: true,
        filterable: true,
    },
    {
        key: 'group',
        label: 'department.column.group',
        field: 'group.name',
        sortable: true,
        filterable: true,
    },
    {
        key: 'description',
        label: 'department.column.description',
        sortable: true,
        filterable: true,
        truncate: true,
        maxWidth: '24rem',
    },
    {
        key: 'users',
        label: 'department.column.users',
        // Sorted and filtered on the association path; the backend sorts collections with listagg.
        field: 'users.username',
        sortable: true,
        filterable: true,
        cellType: 'list',
        listTitle: 'department.column.users',
        listChips: true,
        emptyChipLabel: 'department.column.users.empty',
    },
    {
        key: 'active',
        label: 'department.column.active',
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

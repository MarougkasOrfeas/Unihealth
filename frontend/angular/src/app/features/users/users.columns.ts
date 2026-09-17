import {StatusTone, TableColumnData} from '../../shared/components/base-table/base-table';

/** Backend enum value to the lexicon key shown for it. */
export const USER_STATUS_LABEL = {
    ACTIVE: 'global.status.active',
    DEACTIVATED: 'global.status.deactivated',
    UNVERIFIED: 'global.status.unverified',
} as const;

export type UserStatusName = keyof typeof USER_STATUS_LABEL;

const USER_STATUS_TONE = {
    ACTIVE: 'active',
    DEACTIVATED: 'inactive',
    UNVERIFIED: 'unverified',
} as const satisfies Record<UserStatusName, StatusTone>;

/** A type alias, not an interface: `BaseTableRow` requires an index signature, and only a type
 *  alias of an object literal gets one implicitly. */
export type UserRow = {
    readonly id: string;
    readonly username: string;
    readonly firstName: string;
    readonly lastName: string;
    readonly email: string;
    readonly role: string;
    readonly group: string;
    readonly department: string;
    readonly lastLogin: Date | null;
    /** The raw enum value, not a display label — the facet and the `_page` filter both need it. */
    readonly status: UserStatusName;
};

/** Columns for the users list. */
export const USER_COLUMNS: readonly TableColumnData<UserRow>[] = [
    {key: 'username', label: 'user.column.username', sortable: true, filterable: true},
    {key: 'firstName', label: 'user.column.firstname', field: 'firstname', sortable: true, filterable: true},
    {key: 'lastName', label: 'user.column.lastname', field: 'lastname', sortable: true, filterable: true},
    {key: 'email', label: 'user.column.email', sortable: true, filterable: true},
    {key: 'role', label: 'user.column.role', sortable: true, filterable: true},
    // UserMapper fills these from the association, so the DTO field name and the entity path differ.
    {key: 'group', label: 'user.column.group', field: 'department.group.name', sortable: true, filterable: true},
    {key: 'department', label: 'user.column.department', field: 'department.name', sortable: true, filterable: true},
    {
        key: 'lastLogin',
        label: 'user.column.lastLogin',
        sortable: true,
        // Deliberately not filterable: `_facet` renders a timestamp column as one `to_char` option
        // per distinct login, which is a list of every login rather than a filter.
        cellType: 'date',
        withTime: true,
    },
    {
        key: 'status',
        label: 'user.column.status',
        sortable: true,
        filterable: true,
        facetSearchable: false,
        facetOptions: Object.keys(USER_STATUS_LABEL),
        facetLabel: (value) => USER_STATUS_LABEL[value as UserStatusName] ?? value,
        cellType: 'status',
        status: (row) => ({
            tone: USER_STATUS_TONE[row.status],
            labelKey: USER_STATUS_LABEL[row.status],
        }),
    },
];

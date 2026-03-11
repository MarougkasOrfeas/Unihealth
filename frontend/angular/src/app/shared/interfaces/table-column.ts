export interface TableColumn<T = any> {
    key: keyof T | string;
    header: string;
    sortable?: boolean;
    type?: 'text' | 'date' | 'status' | 'actions';
    cellClass?: string;
    headerClass?: string;
    valueFn?: (row: T) => string;
    actions?: TableAction<T>[];

    filterable?: boolean;
    filterSearchable?: boolean;
}

export interface TableAction<T = any> {
    icon: string;
    tooltip: string;
    disabled?: (row: T) => boolean;
    onClick: (row: T) => void;
}

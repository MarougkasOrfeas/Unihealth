export interface BaseEntity {
    id: string;
    createdBy?: string;
    createdOn?: Date;
}

export interface BaseUpdateableEntity extends BaseEntity {
    modifiedBy?: string;
    modifiedOn?: Date;
}


export type FacetPredicateParams = Partial<Record<string, unknown[]>>;

export type BaseFacetEntity = {
    /**
     * The text term used to filter the returned available options. Use with contains
     */
    columnFilter: string;
    /**
     * The name of the column/field to load the options. Will use the FE UI_BACKEND_MAPPING table same
     * as the filters.
     */
    column: string;
    /**
     * The global search term used to narrow all the entries. (Suchen field)
     */
    search: string;
    locale: string;
    searchStrategy?: string;
    /**
     * Set by record pickers so the backend builds the predicate from the picker's own saved column
     * configuration instead of the management table's.
     */
    pickerMode?: boolean;
    /**
     * The map used to do the global entries filtering. To be used to create the predicate to filter
     * the DB entries similar to _page mapping.
     */
    predicateParams: FacetPredicateParams;
};


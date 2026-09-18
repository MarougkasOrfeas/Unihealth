import {TableColumnData} from '../../shared/components/base-table/base-table';
import {EXAM_CATEGORIES, EXAM_CATEGORY_OPTIONS, ExamCategory} from '../../shared/interfaces/exam-file';

/**
 * A type alias rather than an interface: `BaseTableRow` requires an index signature, and only an
 * object-literal type alias picks one up implicitly.
 */
export type ExamFileRow = {
    readonly id: string;
    /** `PDF`, `JPEG`… derived from the content type in `mapToRow`. */
    readonly fileType: string;
    /** The raw MIME type, carried so the preview can decide how to render it. Not a column. */
    readonly contentType: string;
    readonly name: string;
    /** The raw enum value. What the column keys on, sorts by, facets on and edits. */
    readonly category: ExamCategory | null;
    /** Already translated; see the language-change note on the page component. */
    readonly categoryLabel: string;
    readonly examDate: Date | null;
    /** Pre-formatted, e.g. `1,4 MB`. Sorting still goes to the backend on the raw byte count. */
    readonly fileSizeLabel: string;
};

const CONTENT_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/tiff'] as const;

export const EXAM_FILE_COLUMNS: readonly TableColumnData<ExamFileRow>[] = [
    {
        key: 'fileType',
        label: 'examFile.column.fileType',
        // Sorted and faceted on the raw MIME type; the cell shows the short badge.
        field: 'contentType',
        sortable: true,
        filterable: true,
        facetSearchable: false,
        facetOptions: CONTENT_TYPES,
        facetLabel: (value) => `examFile.contentType.${value}`,
        maxWidth: '7rem',
    },
    {
        key: 'name',
        label: 'examFile.column.name',
        sortable: true,
        filterable: true,
        truncate: true,
        maxWidth: '24rem',
    },
    {
        // Keys on the raw value so sorting, faceting and the editor all agree; `display` is what
        // puts the translated name in the cell.
        key: 'category',
        label: 'examFile.column.category',
        display: (row) => row.categoryLabel,
        editor: {kind: 'select', options: EXAM_CATEGORY_OPTIONS},
        sortable: true,
        filterable: true,
        facetSearchable: false,
        facetOptions: EXAM_CATEGORIES,
        facetLabel: (value) => `examFile.category.${value}`,
    },
    {
        key: 'examDate',
        label: 'examFile.column.examDate',
        sortable: true,
        cellType: 'date',
        // A test cannot have happened in the future, matching the upload form's own bound.
        editor: {kind: 'date', max: new Date()},
        // No time on a lab report date, so `withTime` is left off.
        filterable: true,
        facetSearchable: false,
        /*
         * The filter is by year, but the column sorts by the full date.
         *
         * Facet values are produced and consumed as strings, so faceting the date itself would put
         * every individual examination date in the dropdown. `examYear` is a derived backend field
         * that exists for exactly this.
         */
        facetField: 'examYear',
        maxWidth: '10rem',
    },
    {
        key: 'fileSizeLabel',
        label: 'examFile.column.fileSize',
        // Sorted on the raw number: ordering the formatted strings would put 900 KB after 1,4 MB.
        field: 'fileSize',
        sortable: true,
        maxWidth: '7rem',
    },
];

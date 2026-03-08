/**
 * Generic pagination wrapper matching Spring Data Page<T>.
 *
 * @typeParam T - The type of elements contained in the page.
 *
 * This interface represents a single page of results returned
 * from the backend.
 */
export interface Page<T> {
    /**
     * Content of the current page.
     */
    content: T[];
    /**
     * Total number of elements
     */
    totalElements: number;
    /**
     * Page size.
     */
    size: number;
    /**
     * Index of the current page.
     */
    number: number;
}
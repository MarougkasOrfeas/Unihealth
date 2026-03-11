// paginator-intl.ts
import {Injectable} from '@angular/core';
import {MatPaginatorIntl} from '@angular/material/paginator';

@Injectable()
export class GreekPaginator extends MatPaginatorIntl {
    override itemsPerPageLabel = 'Εγγραφές ανά σελίδα:';
    override nextPageLabel = 'Επόμενη σελίδα';
    override previousPageLabel = 'Προηγούμενη σελίδα';
    override firstPageLabel = 'Πρώτη σελίδα';
    override lastPageLabel = 'Τελευταία σελίδα';

    override getRangeLabel = (page: number, pageSize: number, length: number) => {
        if (length === 0 || pageSize === 0) return `0 από ${length}`;
        const startIndex = page * pageSize;
        const endIndex = Math.min(startIndex + pageSize, length);
        return `${startIndex + 1}–${endIndex} από ${length}`;
    };
}

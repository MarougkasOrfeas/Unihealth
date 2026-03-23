import {Injectable, inject} from '@angular/core';
import {MatPaginatorIntl} from '@angular/material/paginator';
import {TranslateService} from '@ngx-translate/core';

@Injectable()
export class Paginator extends MatPaginatorIntl {
    private translate = inject(TranslateService);

    constructor() {
        super();

        this.translateLabels();

        this.translate.onLangChange.subscribe(() => {
            this.translateLabels();
            this.changes.next();
        });
    }

    private translateLabels(): void {
        this.itemsPerPageLabel = this.translate.instant('paginator.itemsPerPage');
        this.nextPageLabel = this.translate.instant('paginator.nextPage');
        this.previousPageLabel = this.translate.instant('paginator.previousPage');
        this.firstPageLabel = this.translate.instant('paginator.firstPage');
        this.lastPageLabel = this.translate.instant('paginator.lastPage');
    }

    override getRangeLabel = (page: number, pageSize: number, length: number): string => {
        if (length === 0 || pageSize === 0) {
            return `0 ${this.translate.instant('paginator.of')} ${length}`;
        }

        const startIndex = page * pageSize;
        const endIndex = Math.min(startIndex + pageSize, length);

        return `${startIndex + 1}–${endIndex} ${this.translate.instant('paginator.of')} ${length}`;
    };
}

import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {MatSort, MatSortModule, Sort} from '@angular/material/sort';
import {MatPaginator, MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';
import {TableColumn} from "../../interfaces/table-column";
import {DatePipe} from "@angular/common";
import {ColumnFilterComponent} from "../column-filter/column-filter";


@Component({
    selector: 'app-table',
    standalone: true,
    imports: [
        MatTableModule,
        MatSortModule,
        MatPaginatorModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        DatePipe,
        ColumnFilterComponent
    ],
    templateUrl: './table.html',
    styleUrls: ['./table.scss']
})
export class Table<T extends Record<string, any>> implements AfterViewInit, OnChanges {
    @Input() columns: TableColumn<T>[] = [];
    @Input() data: T[] = [];
    @Input() totalElements = 0;
    @Input() pageSize = 10;
    @Input() pageSizeOptions: number[] = [10, 20, 50];
    @Input() loading = false;
    @Input() facetOptions: Record<string, string[]> = {};
    @Input() facetSelection: Record<string, Set<string>> = {};


    @Output() pageChange = new EventEmitter<PageEvent>();
    @Output() sortChange = new EventEmitter<Sort>();
    @Output() facetOpened = new EventEmitter<string>();
    @Output() facetChange = new EventEmitter<{ key: string; selection: Set<string> }>();

    @ViewChild(MatSort) sort!: MatSort;
    @ViewChild(MatPaginator) paginator!: MatPaginator;

    dataSource = new MatTableDataSource<T>([]);
    readonly emptySelection = new Set<string>();

    get displayedColumns(): string[] {
        return this.columns.map(col => String(col.key));
    }

    getFacetOptions(key: string) {
        return this.facetOptions[key] ?? [];
    }

    getFacetSelection(key: string) {
        return this.facetSelection[key] ?? this.emptySelection;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['data']) {
            this.dataSource.data = this.data ?? [];
        }
    }

    ngAfterViewInit(): void {
        this.dataSource.sort = this.sort;
    }

    onSortChanged(sort: Sort): void {
        this.sortChange.emit(sort);
    }

    onPageChanged(event: PageEvent): void {
        this.pageChange.emit(event);
    }

    onFacetOpened(key: string): void {
        this.facetOpened.emit(key);
    }

    onFacetChange(key: string, selection: Set<string>): void {
        this.facetChange.emit({key, selection});
    }

    getCellValue(row: T, column: TableColumn<T>): any {
        if (column.valueFn) {
            return column.valueFn(row);
        }
        return row[column.key as keyof T];
    }

    protected readonly String = String;
}

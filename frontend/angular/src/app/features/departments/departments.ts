import {Component, OnInit} from "@angular/core";
import {Table} from "../../shared/components/table/table";
import {MatPaginatorIntl, PageEvent} from "@angular/material/paginator";
import {GreekPaginator} from "../../shared/components/i18n/paginator-gr";
import {TableColumn} from "../../shared/interfaces/table-column";
import {Sort} from "@angular/material/sort";
import {DepartmentService} from "../../shared/services/department.service";
import {DepartmentDTO} from "../../shared/interfaces/department";
import {MatButton} from "@angular/material/button";
import {MatIcon} from "@angular/material/icon";
import {TranslatePipe} from "@ngx-translate/core";
import {MatTooltip} from "@angular/material/tooltip";


interface DepartmentRow {
    id: string;
    name: string;
    description: string;
    users: string[];
}

@Component({
    selector: 'app-departments',
    standalone: true,
    imports: [Table, MatButton, MatIcon, TranslatePipe, MatTooltip],
    providers: [{provide: MatPaginatorIntl, useClass: GreekPaginator}],
    templateUrl: './departments.html',
    styleUrls: ['./departments.scss']
})
export class Departments implements OnInit {
    departments: DepartmentRow[] = [];
    totalElements = 0;

    isLoading = false;

    currentPageIndex = 0;
    currentPageSize = 10;

    currentSortActive = 'name';
    currentSortDirection: 'asc' | 'desc' = 'asc';

    columns: TableColumn<DepartmentRow>[] = [
        {key: 'name', header: 'Name', sortable: true, filterable: true, filterSearchable: true},
        {key: 'description', header: 'Description', sortable: true, filterable: true, filterSearchable: true},
        {key: 'users', header: 'Users', sortable: true, filterable: true, filterSearchable: true},
        {
            key: 'action',
            header: 'Action',
            type: 'actions',
            actions: [
                {
                    icon: 'edit',
                    tooltip: 'Edit a department',
                    onClick: (row) => this.onEditDepartment(row.id)
                }
            ]
        }
    ];

    facetOptions: Record<string, string[]> = {
        name: [],
        description: [],
    };

    facetSelection: Record<string, Set<string>> = {
        name: new Set<string>(),
        description: new Set<string>(),
    };

    constructor(private departmentService: DepartmentService) {
    }

    ngOnInit(): void {
        this.loadDepartments();
    }

    onPageChange(event: PageEvent): void {
        this.currentPageIndex = event.pageIndex;
        this.currentPageSize = event.pageSize;
        this.loadDepartments();
    }

    onSortChange(sort: Sort): void {
        this.currentSortActive = sort.active || 'username';
        this.currentSortDirection = (sort.direction as 'asc' | 'desc') || 'asc';
        this.currentPageIndex = 0;
        this.loadDepartments();
    }

    onEditDepartment(id: string): void {
        console.log('edit user', id);
    }

    onFacetOpened(key: string): void {
        console.log('facet opened', key);
    }

    onFacetChange(event: { key: string; selection: Set<string> }): void {
        console.log('facet changed', event.key, event.selection);
    }

    onCreate() {
    }

    onCancel() {
    }

    private loadDepartments(): void {
        this.isLoading = true;

        const requestBody = this.buildRequestBody();

        this.departmentService.getPage(requestBody).subscribe({
            next: (page) => {
                this.departments = (page.content ?? []).map(user => this.toRow(user));
                this.totalElements = page.totalElements ?? 0;
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load users', err);
                this.departments = [];
                this.totalElements = 0;
                this.isLoading = false;
            }
        });
    }

    private buildRequestBody(): Record<string, unknown> {
        return {
            page: this.currentPageIndex,
            size: this.currentPageSize,
            sort: [this.buildSortParameter()]
        };
    }

    private buildSortParameter(): string {
        const columnMapping: Record<string, string> = {
            name: 'name',
            description: 'description',
            departments: 'departments',
        };

        const backendField = columnMapping[this.currentSortActive] || 'name';
        return `${backendField},${this.currentSortDirection}`;
    }

    private toRow(d: DepartmentDTO): DepartmentRow {
        return {
            id: (d as any).id ?? '',
            name: d.name,
            description: d.description,
            users: Array.isArray(d.users) ? d.users : [],
        };
    }
}

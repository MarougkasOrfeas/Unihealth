import {Component, OnInit} from "@angular/core";
import {Table} from "../../shared/components/table/table";
import {PageEvent} from "@angular/material/paginator";
import {TableColumn} from "../../shared/interfaces/table-column";
import {GroupService} from "../../shared/services/group.service";
import {Sort} from "@angular/material/sort";
import {GroupDTO} from "../../shared/interfaces/group";
import {MatButton} from "@angular/material/button";
import {MatIcon} from "@angular/material/icon";
import {TranslatePipe} from "@ngx-translate/core";
import {MatTooltip} from "@angular/material/tooltip";
import {Router} from "@angular/router";

interface GroupRow {
    id: string;
    name: string;
    description: string;
    departments: string[];
}


@Component({
    selector: 'app-groups',
    standalone: true,
    imports: [Table, MatButton, MatIcon, TranslatePipe, MatTooltip],
    templateUrl: './groups.html',
    styleUrls: ['./groups.scss']
})
export class Groups implements OnInit {

    groups: GroupRow[] = [];
    totalElements = 0;

    isLoading = false;

    currentPageIndex = 0;
    currentPageSize = 10;
    currentSortActive = 'name';
    currentSortDirection: 'asc' | 'desc' = 'asc';

    columns: TableColumn<GroupRow>[] = [
        {key: 'name', header: 'Name', sortable: true, filterable: true, filterSearchable: true},
        {key: 'description', header: 'Description', sortable: true, filterable: true, filterSearchable: true},
        {key: 'departments', header: 'Departments', sortable: true, filterable: true, filterSearchable: true},
        {
            key: 'action',
            header: 'Action',
            type: 'actions',
            actions: [
                {
                    icon: 'edit',
                    tooltip: 'Edit a group',
                    onClick: (row) => this.onEditGroup(row.id)
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

    constructor(private groupService: GroupService, private readonly router: Router,) {
    }

    ngOnInit(): void {
        this.loadGroups();
    }

    onPageChange(event: PageEvent): void {
        this.currentPageIndex = event.pageIndex;
        this.currentPageSize = event.pageSize;
        this.loadGroups();
    }

    onSortChange(sort: Sort): void {
        this.currentSortActive = sort.active || 'username';
        this.currentSortDirection = (sort.direction as 'asc' | 'desc') || 'asc';
        this.currentPageIndex = 0;
        this.loadGroups();
    }

    onEditGroup(id: string): void {
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
        this.router.navigateByUrl('/');
    }

    private loadGroups(): void {
        this.isLoading = true;

        const requestBody = this.buildRequestBody();

        this.groupService.getPage(requestBody).subscribe({
            next: (page) => {
                this.groups = (page.content ?? []).map(user => this.toRow(user));
                this.totalElements = page.totalElements ?? 0;
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load users', err);
                this.groups = [];
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

    private toRow(g: GroupDTO): GroupRow {
        return {
            id: (g as any).id ?? '',
            name: g.name,
            description: g.description,
            departments: Array.isArray(g.departments) ? g.departments : [],
        };
    }

}

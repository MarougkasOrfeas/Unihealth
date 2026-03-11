import {Component, OnInit} from '@angular/core';
import {MatPaginatorIntl, PageEvent} from '@angular/material/paginator';
import {Sort} from '@angular/material/sort';
import {Table} from "../../shared/components/table/table";
import {TableColumn} from "../../shared/interfaces/table-column";
import {User} from "../../shared/interfaces/user";
import {UserService} from "../../shared/services/user.service";
import {GreekPaginator} from "../../shared/components/i18n/paginator-gr";
import {MatIcon} from "@angular/material/icon";
import {MatTooltip} from "@angular/material/tooltip";
import {TranslatePipe} from "@ngx-translate/core";
import {MatButton} from "@angular/material/button";
import {Router} from "@angular/router";

interface UserRow {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    role: string;
    department: string;
    lastLogin: string;
    status: string;
}

@Component({
    selector: 'app-users',
    standalone: true,
    imports: [Table, MatIcon, MatTooltip, TranslatePipe, MatButton],
    providers: [{provide: MatPaginatorIntl, useClass: GreekPaginator}],
    templateUrl: './users.html',
    styleUrls: ['./users.scss']
})
export class Users implements OnInit {
    users: UserRow[] = [];
    totalElements = 0;

    isLoading = false;

    currentPageIndex = 0;
    currentPageSize = 10;

    currentSortActive = 'username';
    currentSortDirection: 'asc' | 'desc' = 'asc';

    columns: TableColumn<UserRow>[] = [
        {key: 'username', header: 'Username', sortable: true, filterable: true, filterSearchable: true},
        {key: 'firstName', header: 'Firstname', sortable: true, filterable: true, filterSearchable: true},
        {key: 'lastName', header: 'Lastname', sortable: true, filterable: true, filterSearchable: true},
        {key: 'email', header: 'Email', sortable: true, filterable: true, filterSearchable: true},
        {key: 'role', header: 'Role', sortable: true, filterable: true, filterSearchable: true},
        {key: 'department', header: 'Department', sortable: true, filterable: true, filterSearchable: true},
        {key: 'lastLogin', header: 'Last Login', sortable: true},
        {key: 'status', header: 'Status', type: 'status', sortable: true, filterable: true, filterSearchable: false},
        {
            key: 'action',
            header: 'Action',
            type: 'actions',
            actions: [
                {
                    icon: 'edit',
                    tooltip: 'Edit a user',
                    onClick: (row) => this.onEditUser(row.id)
                }
            ]
        }
    ];

    facetOptions: Record<string, string[]> = {
        username: [],
        firstName: [],
        lastName: [],
        email: [],
        status: ['Active', 'Deactivated', 'Unverified']
    };

    facetSelection: Record<string, Set<string>> = {
        username: new Set<string>(),
        firstName: new Set<string>(),
        lastName: new Set<string>(),
        role: new Set<string>(),
        department: new Set<string>(),
        email: new Set<string>(),
        status: new Set<string>()
    };

    constructor(private userService: UserService, private readonly router: Router,) {
    }

    ngOnInit(): void {
        this.loadUsers();
    }

    onCreate() {

    }

    onCancel() {
        this.router.navigateByUrl('/');
    }

    onPageChange(event: PageEvent): void {
        this.currentPageIndex = event.pageIndex;
        this.currentPageSize = event.pageSize;
        this.loadUsers();
    }

    onSortChange(sort: Sort): void {
        this.currentSortActive = sort.active || 'username';
        this.currentSortDirection = (sort.direction as 'asc' | 'desc') || 'asc';
        this.currentPageIndex = 0;
        this.loadUsers();
    }

    onEditUser(id: string): void {
        console.log('edit user', id);
    }

    onFacetOpened(key: string): void {
        console.log('facet opened', key);
    }

    onFacetChange(event: { key: string; selection: Set<string> }): void {
        console.log('facet changed', event.key, event.selection);
    }

    private loadUsers(): void {
        this.isLoading = true;

        const requestBody = this.buildRequestBody();

        this.userService.getPage(requestBody).subscribe({
            next: (page) => {
                this.users = (page.content ?? []).map(user => this.toRow(user));
                this.totalElements = page.totalElements ?? 0;
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load users', err);
                this.users = [];
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
            username: 'username',
            firstName: 'firstname',
            lastName: 'lastname',
            role: 'role',
            department: 'department',
            email: 'email',
            lastLogin: 'lastLogin',
            status: 'status'
        };

        const backendField = columnMapping[this.currentSortActive] || 'username';
        return `${backendField},${this.currentSortDirection}`;
    }

    private toRow(user: User): UserRow {
        return {
            id: user.id ?? '',
            username: user.username,
            firstName: user.firstname,
            lastName: user.lastname,
            email: user.email,
            role: user.role,
            department: user.department,
            lastLogin: this.formatLastLogin(user.lastLogin),
            status: this.mapStatusToLabel(user.status)
        };
    }

    private formatLastLogin(value: string | null | undefined): string {
        if (!value) return '-';

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '-';

        const dd = String(date.getDate()).padStart(2, '0');
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const yyyy = date.getFullYear();
        const hh = String(date.getHours()).padStart(2, '0');
        const min = String(date.getMinutes()).padStart(2, '0');

        return `${dd}.${mm}.${yyyy} ${hh}:${min}`;
    }

    private mapStatusToLabel(status: string): string {
        switch (status) {
            case 'ACTIVE':
                return 'Active';
            case 'DEACTIVATED':
                return 'Deactivated';
            case 'UNVERIFIED':
                return 'Unverified';
            default:
                return status || '-';
        }
    }
}

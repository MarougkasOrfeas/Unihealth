import {ChangeDetectionStrategy, Component, DestroyRef, inject, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {filter, switchMap} from 'rxjs';
import {BaseTable, EmptyStateData} from '../../shared/components/base-table/base-table';
import {Button} from '../../shared/components/button/button';
import {PageHeader} from '../../shared/components/page-header/page-header';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';
import {User} from '../../shared/interfaces/user';
import {MessageService} from '../../shared/services/message.service';
import {UserService} from '../../shared/services/user.service';
import {USER_COLUMNS, UserRow, UserStatusName} from './users.columns';

@Component({
    selector: 'app-users',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [BaseTable, PageHeader, Button, TranslatePipe],
    templateUrl: './users.html',
    styleUrls: ['./users.scss'],
})
export class Users {

    private readonly router = inject(Router);
    private readonly messages = inject(MessageService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly userService = inject(UserService);

    private readonly table = viewChild.required(BaseTable<User, UserRow>);

    protected readonly columns = USER_COLUMNS;

    protected readonly emptyState: EmptyStateData = {
        titleKey: 'user.list.empty.title',
        subtitleKey: 'user.list.empty.subtitle',
        titleErrorKey: 'global.list.error.title',
        subtitleErrorKey: 'global.list.error.subtitle',
        buttonLabelKey: 'user.management.action.create',
    };

    protected readonly toRow = (dto: User): UserRow => ({
        id: dto.id ?? '',
        username: dto.username,
        firstName: dto.firstname,
        lastName: dto.lastname,
        email: dto.email,
        role: dto.role,
        group: dto.group ?? '',
        department: dto.department ?? '',
        // BaseService.transform has already parsed the ISO string into a Date.
        lastLogin: dto.lastLogin instanceof Date ? dto.lastLogin : null,
        status: (dto.status as UserStatusName) ?? 'UNVERIFIED',
    });

    protected onCreate(): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_USERS, 'create']);
    }

    protected onView(row: UserRow): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_USERS, row.id]);
    }

    protected onEdit(row: UserRow): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_USERS, row.id, 'edit']);
    }

    protected onDelete(row: UserRow): void {
        this.messages.confirmDelete(UNIHEALTH_CONSTANTS.ENTITY.USER)
            .afterClosed()
            .pipe(
                filter(Boolean),
                switchMap(() => this.userService.delete(row.id)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: () => {
                    this.messages.deleteSuccess(UNIHEALTH_CONSTANTS.ENTITY.USER);
                    this.table().reloadFirstPage();
                },
                error: () => this.messages.deleteError(),
            });
    }

    /** Saves the query before leaving, so coming back restores the filters, sort and page. */
    private navigate(commands: unknown[]): void {
        this.table().saveCurrentState();
        void this.router.navigate(commands);
    }
}

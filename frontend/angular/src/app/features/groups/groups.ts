import {ChangeDetectionStrategy, Component, DestroyRef, inject, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {filter, switchMap} from 'rxjs';
import {BaseTable, EmptyStateData} from '../../shared/components/base-table/base-table';
import {Button} from '../../shared/components/button/button';
import {PageHeader} from '../../shared/components/page-header/page-header';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';
import {GroupDTO} from '../../shared/interfaces/group';
import {GroupService} from '../../shared/services/group.service';
import {MessageService} from '../../shared/services/message.service';
import {GROUP_COLUMNS, GroupRow} from './groups.columns';

@Component({
    selector: 'app-groups',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [BaseTable, PageHeader, Button, TranslatePipe],
    templateUrl: './groups.html',
    styleUrls: ['./groups.scss'],
})
export class Groups {

    private readonly router = inject(Router);
    private readonly messages = inject(MessageService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly groupService = inject(GroupService);

    private readonly table = viewChild.required(BaseTable<GroupDTO, GroupRow>);

    protected readonly columns = GROUP_COLUMNS;

    protected readonly emptyState: EmptyStateData = {
        titleKey: 'group.list.empty.title',
        subtitleKey: 'group.list.empty.subtitle',
        titleErrorKey: 'global.list.error.title',
        subtitleErrorKey: 'global.list.error.subtitle',
        buttonLabelKey: 'group.management.action.create',
    };

    protected readonly toRow = (dto: GroupDTO): GroupRow => ({
        id: dto.id,
        name: dto.name,
        description: dto.description ?? '',
        departments: dto.departments ?? [],
        active: dto.active,
    });

    protected onCreate(): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_GROUPS, 'create']);
    }

    protected onView(row: GroupRow): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_GROUPS, row.id]);
    }

    protected onEdit(row: GroupRow): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_GROUPS, row.id, 'edit']);
    }

    protected onDelete(row: GroupRow): void {
        this.messages.confirmDelete(UNIHEALTH_CONSTANTS.ENTITY.GROUP)
            .afterClosed()
            .pipe(
                filter(Boolean),
                switchMap(() => this.groupService.delete(row.id)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: () => {
                    this.messages.deleteSuccess(UNIHEALTH_CONSTANTS.ENTITY.GROUP);
                    // From page one: removing a row shifts every later one, so the current page
                    // index may no longer point where the user thinks it does.
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

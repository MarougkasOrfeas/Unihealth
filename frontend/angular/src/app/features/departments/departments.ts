import {ChangeDetectionStrategy, Component, DestroyRef, inject, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {filter, switchMap} from 'rxjs';
import {BaseTable, EmptyStateData} from '../../shared/components/base-table/base-table';
import {Button} from '../../shared/components/button/button';
import {PageHeader} from '../../shared/components/page-header/page-header';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';
import {DepartmentDTO} from '../../shared/interfaces/department';
import {DepartmentService} from '../../shared/services/department.service';
import {MessageService} from '../../shared/services/message.service';
import {DEPARTMENT_COLUMNS, DepartmentRow} from './departments.columns';

@Component({
    selector: 'app-departments',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [BaseTable, PageHeader, Button, TranslatePipe],
    templateUrl: './departments.html',
    styleUrls: ['./departments.scss'],
})
export class Departments {

    private readonly router = inject(Router);
    private readonly messages = inject(MessageService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly departmentService = inject(DepartmentService);

    private readonly table = viewChild.required(BaseTable<DepartmentDTO, DepartmentRow>);

    protected readonly columns = DEPARTMENT_COLUMNS;

    protected readonly emptyState: EmptyStateData = {
        titleKey: 'department.list.empty.title',
        subtitleKey: 'department.list.empty.subtitle',
        titleErrorKey: 'global.list.error.title',
        subtitleErrorKey: 'global.list.error.subtitle',
        buttonLabelKey: 'department.management.action.create',
    };

    protected readonly toRow = (dto: DepartmentDTO): DepartmentRow => ({
        id: dto.id,
        name: dto.name,
        description: dto.description ?? '',
        group: dto.group ?? '',
        users: dto.users ?? [],
        active: dto.active,
    });

    protected onCreate(): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_DEPARTMENTS, 'create']);
    }

    protected onView(row: DepartmentRow): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_DEPARTMENTS, row.id]);
    }

    protected onEdit(row: DepartmentRow): void {
        this.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_DEPARTMENTS, row.id, 'edit']);
    }

    protected onDelete(row: DepartmentRow): void {
        this.messages.confirmDelete(UNIHEALTH_CONSTANTS.ENTITY.DEPARTMENT)
            .afterClosed()
            .pipe(
                filter(Boolean),
                switchMap(() => this.departmentService.delete(row.id)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: () => {
                    this.messages.deleteSuccess(UNIHEALTH_CONSTANTS.ENTITY.DEPARTMENT);
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

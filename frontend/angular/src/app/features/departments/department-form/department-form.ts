import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {map, Observable} from 'rxjs';
import {PermissionService} from '../../../core/auth/permission.service';
import {Button} from '../../../shared/components/button/button';
import {ConstantSelectComponent, SelectOption} from '../../../shared/components/constant-select/constant-select';
import {PageHeader} from '../../../shared/components/page-header/page-header';
import {SaveButtonComponent} from '../../../shared/components/save-button/save-button';
import {SectionTitle} from '../../../shared/components/section-title/section-title';
import {StatusToggle} from '../../../shared/components/status-toggle/status-toggle';
import {TextInputComponent} from '../../../shared/components/text-input/text-input';
import {UNIHEALTH_CONSTANTS} from '../../../shared/constants/unihealth.constants';
import {DepartmentDTO} from '../../../shared/interfaces/department';
import {CanLeaveWithUnsavedChanges} from '../../../shared/interfaces/unsaved-changes';
import {DepartmentService} from '../../../shared/services/department.service';
import {GroupService} from '../../../shared/services/group.service';
import {MessageService} from '../../../shared/services/message.service';
import {errorMessageFromHttp} from '../../../shared/utils/http-error.util';

type FormMode = 'create' | 'edit' | 'view';

const ERROR_LABELS = {
    department_name_already_exists: 'department.error.name.exists',
};

/**
 * Create / view / edit form for a department.
 *
 * The school is mandatory: a department with no school is excluded from
 * `GET /department/_active/by-group/{name}` and would therefore never appear in the school ->
 * department cascade on the user form.
 */
@Component({
    selector: 'app-department-form',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        PageHeader,
        TextInputComponent,
        ConstantSelectComponent,
        SaveButtonComponent,
        SectionTitle,
        StatusToggle,
        Button,
    ],
    templateUrl: './department-form.html',
    styleUrls: ['./department-form.scss'],
})
export class DepartmentForm implements OnInit, CanLeaveWithUnsavedChanges {

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly departmentService = inject(DepartmentService);
    private readonly groupService = inject(GroupService);
    private readonly messages = inject(MessageService);
    private readonly translate = inject(TranslateService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly fb = inject(FormBuilder);

    protected readonly mode = this.route.snapshot.data['mode'] as FormMode;
    private readonly id = this.route.snapshot.paramMap.get('id');

    protected readonly saving = signal(false);
    protected readonly submitted = signal(false);
    /** Null until the department is loaded; the status control only renders once known. */
    protected readonly active = signal<boolean | null>(null);
    protected readonly isAdmin = inject(PermissionService).isAdmin;
    protected readonly groupOptions = signal<readonly SelectOption[]>([]);

    protected readonly form = this.fb.nonNullable.group({
        name: ['', [Validators.required, Validators.maxLength(255)]],
        description: ['', [Validators.maxLength(1000)]],
        group: ['', [Validators.required]],
    });

    protected readonly titleKey =
        this.mode === 'create' ? 'department.create.title' : 'department.edit.title';

    ngOnInit(): void {
        if (this.mode === 'view') {
            this.form.disable();
        } else if (this.mode === 'edit') {
            // Department.name is @Column(updatable = false); a rename via PUT is silently ignored.
            this.form.controls.name.disable();
        }

        this.groupService.getActiveGroups()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((groups) => this.groupOptions.set(
                groups.map((group) => ({key: group.name, label: group.name})),
            ));

        if (!this.id) {
            return;
        }

        this.departmentService.get(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (department) => {
                this.form.patchValue({
                    name: department.name,
                    description: department.description ?? '',
                    group: department.group ?? '',
                });
                this.active.set(department.active);
            },
            error: () => {
                this.messages.notFound(UNIHEALTH_CONSTANTS.ENTITY.DEPARTMENT);
                void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_DEPARTMENTS]);
            },
        });
    }

    /** Owns its own prompt, so the route guard can simply delegate. */
    canDeactivate(): boolean | Observable<boolean> {
        if (!this.form.dirty) {
            return true;
        }
        return this.messages.confirmDiscard().afterClosed().pipe(map((leave) => leave === true));
    }

    protected onCancel(): void {
        void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_DEPARTMENTS]);
    }

    /** Applied immediately: `active` is not a form field, it has its own endpoint. */
    protected onStatusChange(active: boolean): void {
        if (!this.id) {
            return;
        }

        this.departmentService.setActive(this.id, active)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (result) => {
                    this.active.set(result);
                    this.messages.updateSuccess(UNIHEALTH_CONSTANTS.ENTITY.DEPARTMENT);
                },
                error: (err) => this.messages.error(
                    errorMessageFromHttp(err, this.translate, ERROR_LABELS)),
            });
    }

    protected onSave(): void {
        this.submitted.set(true);
        if (this.form.invalid || this.saving()) {
            return;
        }

        this.saving.set(true);
        // getRawValue, not value: a disabled control is omitted from `value`, and the backend
        // still expects `name` in the payload even though it ignores changes to it.
        const raw = this.form.getRawValue();
        const dto = {
            name: raw.name,
            description: raw.description,
            group: raw.group,
        } as DepartmentDTO;

        const request$ = this.id
            ? this.departmentService.update(this.id, dto)
            : this.departmentService.create(dto);

        request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: () => {
                this.form.markAsPristine();
                if (this.id) {
                    this.messages.updateSuccess(UNIHEALTH_CONSTANTS.ENTITY.DEPARTMENT);
                } else {
                    this.messages.createSuccess(UNIHEALTH_CONSTANTS.ENTITY.DEPARTMENT);
                }
                void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_DEPARTMENTS]);
            },
            error: (err) => {
                this.saving.set(false);
                this.messages.error(errorMessageFromHttp(err, this.translate, ERROR_LABELS));
            },
        });
    }
}

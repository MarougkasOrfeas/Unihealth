import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {map, Observable} from 'rxjs';
import {PermissionService} from '../../../core/auth/permission.service';
import {Button} from '../../../shared/components/button/button';
import {PageHeader} from '../../../shared/components/page-header/page-header';
import {SaveButtonComponent} from '../../../shared/components/save-button/save-button';
import {SectionTitle} from '../../../shared/components/section-title/section-title';
import {StatusToggle} from '../../../shared/components/status-toggle/status-toggle';
import {TextInputComponent} from '../../../shared/components/text-input/text-input';
import {UNIHEALTH_CONSTANTS} from '../../../shared/constants/unihealth.constants';
import {GroupDTO} from '../../../shared/interfaces/group';
import {CanLeaveWithUnsavedChanges} from '../../../shared/interfaces/unsaved-changes';
import {MessageService} from '../../../shared/services/message.service';
import {GroupService} from '../../../shared/services/group.service';
import {errorMessageFromHttp} from '../../../shared/utils/http-error.util';

type FormMode = 'create' | 'edit' | 'view';

/** Backend business errors this form can explain in the user's own language. */
const ERROR_LABELS = {
    group_name_already_exists: 'group.error.name.exists',
};

/**
 * Create / view / edit form for a school. One component serves all three routes; the mode comes
 * from the route's `data`, so there is no fourth file that only differs by which controls are
 * disabled.
 */
@Component({
    selector: 'app-group-form',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        PageHeader,
        TextInputComponent,
        SaveButtonComponent,
        SectionTitle,
        StatusToggle,
        Button,
    ],
    templateUrl: './group-form.html',
    styleUrls: ['./group-form.scss'],
})
export class GroupForm implements OnInit, CanLeaveWithUnsavedChanges {

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly groupService = inject(GroupService);
    private readonly messages = inject(MessageService);
    private readonly translate = inject(TranslateService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly fb = inject(FormBuilder);

    protected readonly mode = this.route.snapshot.data['mode'] as FormMode;
    private readonly id = this.route.snapshot.paramMap.get('id');

    protected readonly saving = signal(false);
    protected readonly submitted = signal(false);
    /** Null until the school is loaded; the status control only renders once known. */
    protected readonly active = signal<boolean | null>(null);
    protected readonly isAdmin = inject(PermissionService).isAdmin;

    protected readonly form = this.fb.nonNullable.group({
        name: ['', [Validators.required, Validators.maxLength(255)]],
        description: ['', [Validators.maxLength(1000)]],
    });

    protected readonly titleKey = this.mode === 'create' ? 'group.create.title' : 'group.edit.title';

    ngOnInit(): void {
        if (this.mode === 'view') {
            this.form.disable();
        } else if (this.mode === 'edit') {
            // UniGroup.name is @Column(updatable = false); JPA drops a renamed name on PUT, so the
            // field must not look editable.
            this.form.controls.name.disable();
        }

        if (!this.id) {
            return;
        }

        this.groupService.get(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (group) => {
                this.form.patchValue({
                    name: group.name,
                    description: group.description ?? '',
                });
                this.active.set(group.active);
            },
            error: () => {
                this.messages.notFound(UNIHEALTH_CONSTANTS.ENTITY.GROUP);
                void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_GROUPS]);
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
        void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_GROUPS]);
    }

    /** Applied immediately: `active` is not a form field, it has its own endpoint. */
    protected onStatusChange(active: boolean): void {
        if (!this.id) {
            return;
        }

        this.groupService.setActive(this.id, active)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (result) => {
                    this.active.set(result);
                    this.messages.updateSuccess(UNIHEALTH_CONSTANTS.ENTITY.GROUP);
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
        const dto = {name: raw.name, description: raw.description} as GroupDTO;

        const request$ = this.id
            ? this.groupService.update(this.id, dto)
            : this.groupService.create(dto);

        request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: () => {
                this.form.markAsPristine();
                if (this.id) {
                    this.messages.updateSuccess(UNIHEALTH_CONSTANTS.ENTITY.GROUP);
                } else {
                    this.messages.createSuccess(UNIHEALTH_CONSTANTS.ENTITY.GROUP);
                }
                void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_GROUPS]);
            },
            error: (err) => {
                this.saving.set(false);
                this.messages.error(errorMessageFromHttp(err, this.translate, ERROR_LABELS));
            },
        });
    }
}

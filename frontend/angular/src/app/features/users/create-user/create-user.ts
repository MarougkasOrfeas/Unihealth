import {
    Component,
    ViewEncapsulation,
    DestroyRef,
    ViewChild,
    TemplateRef,
    inject,
    computed,
    signal,
    OnInit,
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
    FormBuilder,
    Validators,
    ReactiveFormsModule,
    FormGroup,
    FormControl,
} from '@angular/forms';

import {distinctUntilChanged, filter, map, startWith} from 'rxjs/operators';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

import {MAT_DATE_LOCALE, MatNativeDateModule} from '@angular/material/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatListModule} from '@angular/material/list';
import {MatInputModule} from '@angular/material/input';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatIconModule} from '@angular/material/icon';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatRadioModule} from '@angular/material/radio';

import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {HttpErrorResponse} from '@angular/common/http';
import {MatDialog} from '@angular/material/dialog';
import {MatTooltip} from '@angular/material/tooltip';
import {Observable, combineLatest} from 'rxjs';
import {DateAdapter} from '@angular/material/core';
import {TranslateModule} from '@ngx-translate/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CanLeaveWithUnsavedChanges} from "../../../shared/interfaces/unsaved-changes";
import {CustomDateAdapter} from "../../../shared/adapters/custom-date-adapter";
import {UserService} from "../../../shared/services/user.service";
import {TranslationsService, Language} from "../../../shared/services/translation.service";
import {ConfirmationDialog} from "../../../shared/components/confirm-dialog/confirm-dialog";
import {UNIHEALTH_CONSTANTS} from "../../../shared/constants/unihealth.constants";
import {User, UserStatus} from "../../../shared/interfaces/user";
import {ConstantSelectComponent, SelectOption} from "../../../shared/components/constant-select/constant-select";
import {EntityStatus, StatusToggle} from "../../../shared/components/status-toggle/status-toggle";
import {PermissionService} from "../../../core/auth/permission.service";
import {TextInputComponent} from "../../../shared/components/text-input/text-input";
import {SaveButtonComponent} from "../../../shared/components/save-button/save-button";
import {SectionTitle} from "../../../shared/components/section-title/section-title";
import {DatePickerComponent} from "../../../shared/components/date-picker/date-picker";
import {UnsavedChangesBeforeUnloadDirective} from "../../../shared/directives/unsaved-changes-beforeunload.directive";
import {GroupDTO, UniGroupOption} from "../../../shared/interfaces/group";
import {DepartmentDTO, DepartmentOption} from "../../../shared/interfaces/department";
import {GroupService} from "../../../shared/services/group.service";
import {DepartmentService} from "../../../shared/services/department.service";

type DeactivationMode = 'automatic' | 'scheduled';

/** Mirrors the backend `UserRoles` enum. */
type UserRole = 'USER' | 'ADMIN';

type UserForm = FormGroup<{
    firstname: FormControl<string>;
    lastname: FormControl<string>;
    username: FormControl<string>;
    email: FormControl<string>;
    phoneNumber: FormControl<string | null>;
    deactivateAfter: FormControl<Date | null>;
    deactivationMode: FormControl<DeactivationMode>;
    language: FormControl<string>;
    activateUsernameSuggestion: FormControl<boolean>;
    scheduledDeactivationReason: FormControl<string | null>;
    role: FormControl<UserRole>;
    group: FormControl<string>;
    department: FormControl<string>;
}>;

type UserFormField = keyof UserForm['controls'];

@Component({
    selector: 'app-users',
    encapsulation: ViewEncapsulation.None,
    standalone: true,
    imports: [
        ReactiveFormsModule,
        CommonModule,

        MatButtonModule,
        MatCardModule,
        MatListModule,
        MatInputModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatSelectModule,
        MatIconModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatRadioModule,
        MatSnackBarModule,
        MatTooltip,
        TranslateModule,
        MatCheckboxModule,
        ConstantSelectComponent,
        TextInputComponent,
        SaveButtonComponent,
        SectionTitle,
        StatusToggle,
        DatePickerComponent,
        UnsavedChangesBeforeUnloadDirective
    ],
    providers: [{provide: MAT_DATE_LOCALE, useValue: 'el-GR'}, {provide: DateAdapter, useClass: CustomDateAdapter}],
    templateUrl: './create-user.html',
    styleUrl: './create-user.scss',
})
export class CreateUser implements OnInit, CanLeaveWithUnsavedChanges {
    /** True after the user clicked save once (used to show validation errors). */
    submitted = false;
    /** Reactive form used by the create-user screen. */
    form;
    /** Min date for the datepicker. */
    today = new Date();
    /** Default status for new users. */
    defaultStatus: string = 'UNVERIFIED';
    /** Default user pref language. */
    defaultPrefLang: string = 'EL';

    /** Which of the three routes rendered this screen: create, read-only view, or edit. */
    readonly mode = (inject(ActivatedRoute).snapshot.data['mode'] ?? 'create') as
        'create' | 'edit' | 'view';
    /** Id of the user being viewed or edited; null when creating. */
    readonly userId = inject(ActivatedRoute).snapshot.paramMap.get('id');
    /** The loaded user, so fields the form does not edit survive a round trip. */
    private loadedUser: User | null = null;

    /** Mirrors the role control, so the template can hide the school/department section. */
    isAdminRole = false;

    readonly roleOptions: SelectOption<UserRole>[] = [
        {key: 'USER', label: 'user.role.user'},
        {key: 'ADMIN', label: 'user.role.admin'},
    ];

    /**
     * Current status of the loaded user; null until loaded, so the toggle stays hidden.
     *
     * Kept as the backend enum rather than a boolean because UNVERIFIED is a third state: the
     * account is enabled in Keycloak and only waiting on email confirmation.
     */
    readonly status = signal<UserStatus | null>(null);

    /** The status pill's appearance, which doubles as its CSS class. */
    readonly statusTone = computed<EntityStatus>(() => {
        switch (this.status()) {
            case UserStatus.ACTIVE:
                return 'active';
            case UserStatus.UNVERIFIED:
                return 'unverified';
            default:
                return 'inactive';
        }
    });
    /** True when deactivating this user would leave the app with no administrator. */
    readonly lastAdmin = signal(false);

    /** Exposed so the template can compare against the backend enum. */
    readonly UserStatusEnum = UserStatus;

    /**
     * Whether the offered action switches the account off. Anything that is not already deactivated
     * can only be deactivated — an unverified account is enabled, it is just awaiting confirmation.
     */
    private readonly statusDeactivates = computed(
        () => this.status() !== UserStatus.DEACTIVATED,
    );

    /**
     * The last-admin rule only blocks deactivation. Reactivating a deactivated last administrator
     * must stay possible, otherwise the only route back into the administration screens is closed
     * for good.
     */
    readonly statusActionDisabled = computed(
        () => this.statusDeactivates() && this.lastAdmin(),
    );
    readonly isAdmin = inject(PermissionService).isAdmin;

    get isCreate(): boolean {
        return this.mode === 'create';
    }

    get titleKey(): string {
        return this.isCreate ? 'user.management.action.create' : 'user.edit.title';
    }
    readonly dialog = inject(MatDialog);

    /**The modal content to be shown. Address template. */
    @ViewChild('addressTemplate') modalTemplate!: TemplateRef<any>;

    /** Options for the preferred language select in the UI. */
    languageOptions: Language[] = [];
    groups: UniGroupOption[] = [];
    groupOptions: SelectOption<string>[] = [];
    departmentOptions: SelectOption<string>[] = [];

    /**
     * Maps business error codes to readable messages for the user.
     */
    readonly BUSINESS_MESSAGES: Record<string, string> = {
        username_already_exists: 'Το όνομα χρήστη υπάρχει ήδη.',
        email_already_exists: 'Το email υπάρχει ήδη.',
        email_invalid: 'Μη έγκυρη διεύθυνση email.',
        department_required: 'Το τμήμα είναι υποχρεωτικό για χρήστες που δεν είναι διαχειριστές.',
        cannot_deactivate_last_admin: 'Δεν μπορεί να απενεργοποιηθεί ο τελευταίος διαχειριστής.',
    };

    /**
     * Maps validation error to Greek.
     */
    readonly VALIDATION_MESSAGES: Record<string, string> = {
        required: 'Υποχρεωτικό πεδίο',
        email: 'Μη έγκυρη διεύθυνση email',
        maxlength: 'Υπέρβαση μέγιστου μήκους',
    };


    constructor(
        private readonly router: Router,
        private readonly fb: FormBuilder,
        private readonly destroyRef: DestroyRef,
        private readonly userService: UserService,
        private readonly snackBar: MatSnackBar,
        private readonly translationsService: TranslationsService,
        private readonly groupService: GroupService,
        private readonly departmentService: DepartmentService,
    ) {
        // Normalize "today" so date comparisons are stable.
        this.today.setHours(0, 0, 0, 0);
        // Build the form.
        this.form = this.buildForm();
        // Wire up reactive behavior (enable/disable fields based on mode).
        this.setupDeactivationModeListener();

        const usernameCtrl = this.form.controls.username;
        const activateUsernameSuggestionCtrl = this.form.controls.activateUsernameSuggestion;

        activateUsernameSuggestionCtrl.valueChanges.subscribe(v => {
            if (v) {
                this.disableUsernameSuggestion(usernameCtrl);
            } else {
                this.enableUsernameSuggestion(usernameCtrl);
            }
        });

        if (activateUsernameSuggestionCtrl.value) {
            this.disableUsernameSuggestion(usernameCtrl);
        } else {
            this.enableUsernameSuggestion(usernameCtrl);
        }

    }

    ngOnInit(): void {
        this.translationsService.getLanguages().subscribe((languages) => {
            this.languageOptions = languages;
            // Only default the language when creating; an existing user already has one.
            if (this.isCreate) {
                this.form.controls.language.setValue(languages[0].key);
            }
        });

        this.loadGroups();
        // this.loadDepartments();
        this.setupGroupDepartmentLogic();
        this.setupRoleLogic();

        if (this.userId) {
            this.loadUser(this.userId);
        }

        if (this.mode === 'view') {
            this.form.disable();
        } else if (this.mode === 'edit') {
            // UserMapper.mapForUpdate ignores role, so a change here would be silently dropped.
            this.form.controls.role.disable();
            // User.username is @Column(updatable = false); a rename via PUT is silently ignored,
            // so the field must not look editable — and suggesting one makes no sense here.
            this.form.controls.username.disable();
            this.form.controls.activateUsernameSuggestion.setValue(false);
            this.form.controls.activateUsernameSuggestion.disable();
        }

        const firstname$ = this.form.controls.firstname.valueChanges.pipe(startWith(this.form.controls.firstname.value));
        const lastname$ = this.form.controls.lastname.valueChanges.pipe(startWith(this.form.controls.lastname.value));

        combineLatest([firstname$, lastname$]).pipe(
            // Only while creating: an existing user's username cannot change, so suggesting one
            // would be a request per keystroke for a value that can never be applied.
            filter(() => this.isCreate),
            map(([first, last]) => first.length > 0 && last.length > 0),
            filter(bothFilled => bothFilled),
            takeUntilDestroyed(this.destroyRef),
        ).subscribe(() => {
            this.userService.suggestUsername(this.form.controls.firstname.value, this.form.controls.lastname.value).subscribe(suggested => {
                if (this.form.controls.activateUsernameSuggestion.value) {
                    this.form.controls.username.setValue(suggested);
                }
            });
        });
    }

    /**
     * Saves the user.
     * - validates the form
     * - sends data to backend
     * - shows success or error message
     */
    onSave() {
        this.submitted = true;
        // Clear backend errors from previous attempts
        this.clearBackendErrors();
        // Show validation errors
        this.form.markAllAsTouched();
        if (!this.form.valid) return;
        // Create DTO from current form values.
        const dto = this.toUserDto();

        const request$ = this.userId
            ? this.userService.update(this.userId, dto)
            : this.userService.create(dto);

        request$.subscribe({
            next: () => {
                this.showToast(
                    this.userId
                        ? 'Ο χρήστης ενημερώθηκε επιτυχώς.'
                        : 'Ο χρήστης δημιουργήθηκε επιτυχώς.',
                    'success',
                );
                this.form.markAsPristine(); // dont call canDeactivate
                this.goBack();
            },
            error: (err) => this.handleBackendError(err),
        });
    }

    /**
     * Called when the user clicks cancel.
     * Navigates back to the previous page.
     */
    onCancel() {
        if (!this.form.dirty) {
            this.goBack();
            return;
        }
        // This is added to keep things separate with canDeactivate logic. There were confusions with back navigation for this case only.
        this.showConfirmationModal('Όλες οι μη αποθηκευμένες αλλαγές θα χαθούν.', 'Έξοδος').subscribe((leave) => {
            if (!leave) return;
            this.form.markAsPristine(); // dont call canDeactivate
            this.goBack();
        });
    }

    private goBack() {
        this.router.navigateByUrl('/users');
    }

    /**
     * Loads the user behind `:id` and fills the form.
     *
     * The group must be applied before the department: `setupGroupDepartmentLogic` reloads the
     * department options whenever the group changes and clears the selection, so patching both at
     * once would wipe the department straight back out.
     */
    private loadUser(id: string): void {
        this.userService.get(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (user) => {
                this.loadedUser = user;

                this.form.patchValue({
                    firstname: user.firstname,
                    lastname: user.lastname,
                    username: user.username,
                    email: user.email,
                    phoneNumber: user.phoneNumber ?? '',
                    language: user.language ?? '',
                    deactivationMode: user.deactivationMode === 'SCHEDULED' ? 'scheduled' : 'automatic',
                    deactivateAfter: user.deactivateAfter ? new Date(user.deactivateAfter) : null,
                    scheduledDeactivationReason: user.scheduledDeactivationReason ?? null,
                    group: user.group ?? '',
                });

                // Role is patched separately so setupRoleLogic reacts to it and applies the
                // school/department rules before the department value is restored below.
                this.form.controls.role.setValue((user.role as UserRole) ?? 'USER');
                this.form.controls.department.setValue(user.department ?? '');
                this.status.set(user.status as UserStatus);
                this.form.markAsPristine();

                this.loadLastAdminFlag(id);
            },
            error: () => {
                this.showToast('Ο χρήστης δεν βρέθηκε.', 'error');
                this.goBack();
            },
        });
    }

    /**
     * Asks the backend whether this user is the only remaining administrator, which is the one
     * case where the status toggle must stay disabled.
     */
    private loadLastAdminFlag(id: string): void {
        this.userService.isLastAdmin(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (isLast) => this.lastAdmin.set(isLast),
                // Fail closed: if we cannot tell, do not offer to deactivate.
                error: () => this.lastAdmin.set(true),
            });
    }

    /** Applied immediately: the status has its own endpoint and is not a form field. */
    onStatusChange(active: boolean): void {
        if (!this.userId) {
            return;
        }

        const status = active ? UserStatus.ACTIVE : UserStatus.DEACTIVATED;
        this.userService.setUserStatus(this.userId, status, null)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (answer) => {
                    // Trust the answer: activating an unverified account keeps it UNVERIFIED.
                    this.status.set(answer.userStatus as UserStatus);
                    this.showToast(
                        active ? 'Ο χρήστης ενεργοποιήθηκε.' : 'Ο χρήστης απενεργοποιήθηκε.',
                        'success',
                    );
                    this.loadLastAdminFlag(this.userId!);
                },
                error: (err) => this.handleBackendError(err),
            });
    }

    /**
     * Prevents navigation away when there are unsaved changes.
     * Opens a confirmation dialog and proceeds only if the user confirms.
     */
    canDeactivate(): boolean | Observable<boolean> {
        // Allow navigation when there are no unsaved changes
        if (!this.form.dirty) return true;
        // show confirmation modal when leaving with changes.
        return this.showConfirmationModal('Όλες οι μη αποθηκευμένες αλλαγές θα χαθούν.', 'Έξοδος');
    }

    private shouldShowErrors(ctrl: FormControl<any>): boolean {
        return this.submitted || ctrl.touched || ctrl.dirty;
    }

    private loadGroups(): void {
        this.groupService.getActiveGroups().subscribe((groups: GroupDTO[]) => {
            this.groupOptions = groups.map(group => ({
                key: group.name,
                label: group.name,
            }));
        });
    }

    // private loadDepartments(): void {
    //     this.departmentService.getActiveDepartments().subscribe(departments => {
    //         this.departments = departments;
    //     });
    // }

    /**
     * An administrator is staff, not a student, so they belong to no school or department. Picking
     * ADMIN clears both selects, disables them and drops their `required` validators; picking USER
     * restores all three. The backend applies the same rule in `UserServiceImpl.validateForCreate`.
     */
    private setupRoleLogic(): void {
        const roleCtrl = this.form.controls.role;
        const groupCtrl = this.form.controls.group;
        const departmentCtrl = this.form.controls.department;

        roleCtrl.valueChanges
            .pipe(startWith(roleCtrl.value), distinctUntilChanged(),
                takeUntilDestroyed(this.destroyRef))
            .subscribe((role) => {
                this.isAdminRole = role === 'ADMIN';

                if (this.isAdminRole) {
                    groupCtrl.clearValidators();
                    departmentCtrl.clearValidators();
                    groupCtrl.setValue('', {emitEvent: false});
                    departmentCtrl.setValue('', {emitEvent: false});
                    groupCtrl.disable({emitEvent: false});
                    departmentCtrl.disable({emitEvent: false});
                } else {
                    groupCtrl.setValidators([Validators.required]);
                    departmentCtrl.setValidators([Validators.required]);
                    groupCtrl.enable({emitEvent: false});
                    // The department stays disabled until a school is chosen; that is owned by
                    // setupGroupDepartmentLogic, so it is not enabled here.
                }

                groupCtrl.updateValueAndValidity({emitEvent: false});
                departmentCtrl.updateValueAndValidity({emitEvent: false});
            });
    }

    private setupGroupDepartmentLogic(): void {
        const groupCtrl = this.form.controls.group;
        const departmentCtrl = this.form.controls.department;

        groupCtrl.valueChanges
            .pipe(
                startWith(groupCtrl.value),
                distinctUntilChanged(),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe((groupName) => {
                departmentCtrl.reset('', {emitEvent: false});

                if (!groupName) {
                    this.departmentOptions = [];
                    departmentCtrl.disable({emitEvent: false});
                    return;
                }

                this.departmentService.getActiveDepartmentsByGroup(groupName).subscribe((departments: DepartmentDTO[]) => {
                    this.departmentOptions = departments.map(department => ({
                        key: department.name,
                        label: department.name,
                    }));

                    if (this.departmentOptions.length > 0) {
                        departmentCtrl.enable({emitEvent: false});
                    } else {
                        departmentCtrl.disable({emitEvent: false});
                    }
                });
            });
    }

    /**
     * Opens a confirmation dialog and returns
     * true if navigation is allowed, false otherwise.
     */
    private showConfirmationModal(message: string, confirmText: string): Observable<boolean> {
        return this.dialog.open(ConfirmationDialog, {
            disableClose: true,
            panelClass: 'confirm-exit-dialog',
            data: {
                message: message,
                confirmText: confirmText,
            },
        }).afterClosed().pipe(map((leave: boolean) => leave));
    }

    /**
     * Creates and returns the form with all controls and validators.
     */
    private buildForm(): UserForm {
        // Build the form with defaults and validators.
        return this.fb.nonNullable.group({
            firstname: ['', {
                updateOn: 'blur',
                validators: [Validators.required, Validators.pattern(UNIHEALTH_CONSTANTS.PATTERNS.FIRSTNAME_LASTNAME_PTN), Validators.minLength(2), Validators.maxLength(50)]
            }],
            lastname: ['', {
                updateOn: 'blur',
                validators: [Validators.required, Validators.pattern(UNIHEALTH_CONSTANTS.PATTERNS.FIRSTNAME_LASTNAME_PTN), Validators.minLength(2), Validators.maxLength(50)]
            }],
            username: ['', [Validators.required, Validators.pattern(UNIHEALTH_CONSTANTS.PATTERNS.USERNAME_PTN), Validators.minLength(3), Validators.maxLength(20)]],
            email: ['', [Validators.required, Validators.pattern(UNIHEALTH_CONSTANTS.PATTERNS.EMAIL_PTN), Validators.maxLength(255)]],
            phoneNumber: ['', [Validators.pattern(UNIHEALTH_CONSTANTS.PATTERNS.PHONE_PTN), Validators.minLength(5), Validators.maxLength(30)]],
            // Disabled by default. Enabled only when "scheduled" is selected.
            deactivateAfter: this.fb.control<Date | null>({value: null, disabled: true}),

            deactivationMode: this.fb.nonNullable.control<DeactivationMode>('automatic', {
                validators: [Validators.required],
            }),
            language: ['', [Validators.required]],
            activateUsernameSuggestion: this.fb.nonNullable.control<boolean>(false),
            scheduledDeactivationReason: this.fb.control<string | null>(null, {
                validators: [Validators.maxLength(255)],
            }),
            role: this.fb.nonNullable.control<UserRole>('USER', {
                validators: [Validators.required],
            }),
            group: this.fb.nonNullable.control('', {
                validators: [Validators.required],
            }),
            department: this.fb.nonNullable.control(
                {value: '', disabled: true},
                {validators: [Validators.required]}
            ),
        }) as UserForm;
    }

    /**
     * Subscribes to the deactivation mode and applies the form rules.
     * This keeps the date field enabled/disabled automatically.
     */
    private setupDeactivationModeListener() {
        const modeCtrl = this.form.controls.deactivationMode;

        // React to changes + apply once for the initial value.
        modeCtrl.valueChanges
            .pipe(startWith(modeCtrl.value), takeUntilDestroyed(this.destroyRef))
            .subscribe((mode) => {
                // Update date field rules whenever the mode changes.
                this.applyDeactivationRules(mode);
            });
    }

    /**
     * Enables/disables the "deactivateAfter" control based on the selected mode.
     *
     * @param mode Current deactivation mode
     */
    private applyDeactivationRules(mode: DeactivationMode) {
        const dateCtrl = this.form.controls.deactivateAfter;
        const reasonCtrl = this.form.controls.scheduledDeactivationReason;
        if (mode === 'scheduled') {
            // Scheduled: user must pick a date.
            dateCtrl.enable({emitEvent: false});
            reasonCtrl.enable({emitEvent: false});
            const currentValidators = dateCtrl.validator;
            if (currentValidators) {
                dateCtrl.setValidators(Validators.compose([Validators.required, currentValidators]));
            } else {
                dateCtrl.setValidators(Validators.required);
            }
        } else {
            // Automatic: date should be disabled and empty.
            // dateCtrl.clearValidators();
            dateCtrl.reset(null, {emitEvent: false});
            dateCtrl.disable({emitEvent: false});
            reasonCtrl.reset(null, {emitEvent: false});
            reasonCtrl.disable({emitEvent: false});
        }
        // Recalculate validation state.
        dateCtrl.updateValueAndValidity({emitEvent: false});
    }

    /**
     * Builds the backend DTO from the current form values.
     * Converts dates and maps enum values.
     */
    private toUserDto(): User {
        const raw = this.form.getRawValue();
        return {
            id: this.loadedUser?.id ?? '',
            username: raw.username,
            email: raw.email,
            firstname: raw.firstname,
            lastname: raw.lastname,
            // Phone number is optional
            phoneNumber: raw.phoneNumber?.trim() || null,
            // Only send date when scheduled and date exists.
            deactivateAfter:
                raw.deactivationMode === 'scheduled' && raw.deactivateAfter
                    ? this.toIsoDate(raw.deactivateAfter)
                    : null,
            // Map UI values to backend enum strings.
            deactivationMode: this.mapDeactivationMode(raw.deactivationMode),
            language: raw.language?.trim() || '',
            group: raw.group?.trim() || '',
            department: raw.department?.trim() || '',
            // Carried from the loaded user when editing rather than reset to create-time defaults.
            // UserMapper.mapForUpdate ignores status and role anyway, so this is about not sending
            // a payload that misstates the record.
            lastLogin: this.loadedUser?.lastLogin ?? '',
            // On create the chosen role is authoritative; on update the backend ignores it, so the
            // loaded value is echoed back rather than the (disabled) control's.
            role: this.loadedUser?.role ?? raw.role,
            status: this.loadedUser?.status ?? this.defaultStatus,
            deactivatedDueToInactivity: this.loadedUser?.deactivatedDueToInactivity ?? false,
            scheduledDeactivationReason: raw.scheduledDeactivationReason?.trim() || null,
        };
    }

    /**
     * Converts a JS Date into "yyyy-MM-dd" (ISO date, without time).
     *
     * @param d Date object
     * @returns ISO date string
     */
    private toIsoDate(d: Date): string {
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    /**
     * Maps the UI deactivation mode to the backend enum value.
     *
     * @param mode UI mode
     * @returns Backend enum string or null if not set
     */
    private mapDeactivationMode(mode: DeactivationMode) {
        switch (mode) {
            case 'automatic':
                return 'AUTOMATIC';
            case 'scheduled':
                return 'SCHEDULED';
            default:
                return 'AUTOMATIC';
        }
    }

    /**
     * Shows a toaster message.
     * Used for both success and error.
     *
     * @param message Text shown to the user
     * @param type Success or Error toast to display.
     */
    private showToast(message: string, type: 'success' | 'error' = 'success') {
        this.snackBar.open(message, UNIHEALTH_CONSTANTS.TOAST.ACTION_LABEL_KEY, {
            duration: UNIHEALTH_CONSTANTS.TOAST.DURATION_MS,
            horizontalPosition: UNIHEALTH_CONSTANTS.TOAST.HORIZONTAL_POSITION,
            verticalPosition: UNIHEALTH_CONSTANTS.TOAST.VERTICAL_POSITION,
            panelClass: [`unihealth-snackbar-${type}`],
        });
    }

    /**
     * Handles backend errors after save.
     * - Shows a toast message
     * - Marks the related form field (if possible)
     */
    private handleBackendError(err: HttpErrorResponse) {
        // Bean Validation errors (DTO annotations)
        if (this.tryHandleValidationErrors(err)) return;

        // Business errors (string labels like "username_already_exists")
        const label = this.extractBackendLabel(err);
        if (!label) {
            this.showToast('Η αποθήκευση απέτυχε.', 'error');
            return;
        }
        // Proceed on identifying business exception
        if (this.tryHandleBusinessLabel(label)) return;

        // For Unknown label show mapped message or raw label
        this.showToast(this.BUSINESS_MESSAGES[label] ?? label, 'error');
    }

    /**
     * Handles validation errors coming from DTO annotations.
     * Returns true if a validation error was handled.
     */
    private tryHandleValidationErrors(err: HttpErrorResponse): boolean {
        const errors = this.extractValidationErrors(err);
        if (!errors?.length) return false;

        const first = errors[0] as { field?: string };
        const fieldName = first.field?.trim();
        const msg = this.formErrorMessages(fieldName ?? '');
        // Mark the related form field, if backend sent one
        const ctrl = fieldName && this.form.contains(fieldName) ? this.form.get(fieldName) : null;

        if (ctrl instanceof FormControl) {
            const message =
                (ctrl.hasError('required') && this.VALIDATION_MESSAGES['required']) ||
                (ctrl.hasError('email') && this.VALIDATION_MESSAGES['email']) ||
                (ctrl.hasError('maxlength') && this.VALIDATION_MESSAGES['maxlength']) ||
                'Μη έγκυρη εισαγωγή.';

            this.setBackendError(ctrl, message);
        }

        this.showToast(msg, 'error');
        return true;
    }

    /**
     * Handles known business error labels (plain string response).
     * Returns true if the label was handled.
     */
    private tryHandleBusinessLabel(label: string): boolean {
        switch (label) {
            case 'username_already_exists':
                this.setBackendError(this.form.controls.username, this.BUSINESS_MESSAGES[label]);
                this.showToast(this.BUSINESS_MESSAGES[label], 'error');
                return true;

            case 'email_already_exists':
                this.setBackendError(this.form.controls.email, this.BUSINESS_MESSAGES[label]);
                this.showToast(this.BUSINESS_MESSAGES[label], 'error');
                return true;

            case 'email_invalid':
                this.setBackendError(this.form.controls.email, this.BUSINESS_MESSAGES[label]);
                this.showToast(this.BUSINESS_MESSAGES[label], 'error');
                return true;

            default:
                return false;
        }
    }

    /**
     * Reads validation errors from the backend.
     * Backend can send:
     * - JSON array (normal)
     * - stringified JSON array (when responseType is "text")
     */
    private extractValidationErrors(err: HttpErrorResponse) {
        const body = err?.error;
        // Use validation errors from backend
        if (Array.isArray(body)) return body;
        // fallback: try parse the response if is JSON
        if (typeof body === 'string' && body.trim().startsWith('[')) {
            try {
                const parsed = JSON.parse(body);
                return Array.isArray(parsed) ? parsed : null;
            } catch {
                return null;
            }
        }
        return null;
    }

    /**
     * Reads a backend error label when the backend returns plain text.
     */
    private extractBackendLabel(err: HttpErrorResponse): string | null {
        const body = err?.error;
        return typeof body === 'string' && body.trim().length ? body.trim() : null;
    }

    /**
     * Marks a control with a custom backend error so mat-error can show it.
     */
    private setBackendError(ctrl: FormControl<any>, message: string,) {
        ctrl.setErrors({...(ctrl.errors ?? {}), backend: message,});
        ctrl.markAsTouched();
    }

    /**
     * Clears only our custom backend errors before a new save attempt.
     */
    private clearBackendErrors() {
        // only clear our custom backend error; keep other validator errors intact
        const controls = [this.form.controls.username, this.form.controls.email];

        for (const c of controls) {
            if (c.hasError('backend')) {
                const errs = {...(c.errors ?? {})};
                delete errs['backend'];
                c.setErrors(Object.keys(errs).length ? errs : null);
            }
        }
    }


    /**
     * For toast pop message errors based on the field.
     * @param invalidField
     * @returns error message.
     */
    private formErrorMessages(invalidField: string) {
        switch (invalidField) {
            case 'email':
                return 'Μη έγκυρη διεύθυνση email.';
            case 'firstname':
                return 'Μη έγκυρο όνομα.';
            case 'lastname':
                return 'Μη έγκυρο επώνυμο.';
            case 'phonenumber':
                return 'Μη έγκυρος αριθμός τηλεφώνου.'
            default:
                return 'Μη έγκυρη εισαγωγή.';
        }
    }

    /**
     * Disables the username control and clears its value for auto-suggestion mode.
     */
    private disableUsernameSuggestion(usernameCtrl: FormControl<string>): void {
        usernameCtrl.disable({emitEvent: false});
        usernameCtrl.setValue('');
    }

    /**
     * Enables the username control for manual input.
     */
    private enableUsernameSuggestion(usernameCtrl: FormControl<string>): void {
        usernameCtrl.enable({emitEvent: false});
    }

    checkUsernameExists(): void {
        const username = this.form.controls.username.value;
        if (!username) {
            return;
        }
        this.userService.checkUsernameExists(username).subscribe(exists => {
            if (exists) {
                this.setBackendError(this.form.controls.username, 'Το όνομα χρήστη υπάρχει ήδη.');
                this.showToast('Το όνομα χρήστη υπάρχει ήδη.', 'error');
            }
        });
    }
}

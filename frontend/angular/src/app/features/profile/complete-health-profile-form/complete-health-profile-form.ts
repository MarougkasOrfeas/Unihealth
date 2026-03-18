import { Component, EventEmitter, inject, OnInit, Output } from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { OptionalHealthProfileDto } from '../../../shared/interfaces/optional-health-profile';
import { OptionalHealthProfileService } from '../../../shared/services/optional-health-profile.service';
import {
    ACTIVITY_LEVEL_OPTIONS,
    DIET_TYPE_OPTIONS,
    FITNESS_LEVEL_OPTIONS,
    HYDRATION_LEVEL_OPTIONS,
    MEAL_REGULARITY_OPTIONS,
    PREFERRED_CONTENT_TYPE_OPTIONS,
    PREFERRED_ROUTINE_TIME_OPTIONS,
    SLEEP_QUALITY_OPTIONS,
    STRESS_LEVEL_OPTIONS,
    STUDY_LOAD_OPTIONS,
    WELLNESS_FOCUS_OPTIONS
} from '../../../shared/types/optional-health-profile.types';
import { HealthProfileService } from '../../../shared/services/health-profile.service';
import { Gender, HealthProfileDTO, PrimaryGoal } from '../../../shared/interfaces/health-profile';

@Component({
    selector: 'app-complete-health-profile-form',
    templateUrl: './complete-health-profile-form.html',
    styleUrls: ['./complete-health-profile-form.scss'],
    imports: [ReactiveFormsModule]
})
export class CompleteHealthProfileForm implements OnInit {
    @Output() optionalSubmit = new EventEmitter<OptionalHealthProfileDto>();

    private readonly fb = inject(FormBuilder);
    private readonly optionalHealthProfileService = inject(OptionalHealthProfileService);
    private readonly healthProfileService = inject(HealthProfileService);

    saving = false;
    loading = false;
    submitted = false;

    errorMessage: string | null = null;
    successMessage: string | null = null;
    optionalErrorMessage: string | null = null;
    optionalSuccessMessage: string | null = null;

    isEditingBase = false;
    isEditingOptional = false;

    initialBaseValue: any = null;
    initialOptionalValue: any = null;

    readonly genders = Object.values(Gender);
    readonly primaryGoals = Object.values(PrimaryGoal);

    readonly sleepQualityOptions = SLEEP_QUALITY_OPTIONS;
    readonly stressLevelOptions = STRESS_LEVEL_OPTIONS;
    readonly studyLoadOptions = STUDY_LOAD_OPTIONS;
    readonly activityLevelOptions = ACTIVITY_LEVEL_OPTIONS;
    readonly dietTypeOptions = DIET_TYPE_OPTIONS;
    readonly mealRegularityOptions = MEAL_REGULARITY_OPTIONS;
    readonly hydrationLevelOptions = HYDRATION_LEVEL_OPTIONS;
    readonly fitnessLevelOptions = FITNESS_LEVEL_OPTIONS;
    readonly preferredRoutineTimeOptions = PREFERRED_ROUTINE_TIME_OPTIONS;
    readonly preferredContentTypeOptions = PREFERRED_CONTENT_TYPE_OPTIONS;
    readonly wellnessFocusOptions = WELLNESS_FOCUS_OPTIONS;

    readonly baseForm = this.fb.group(
        {
            dateOfBirth: [null as string | null, Validators.required],
            gender: [null as Gender | null, Validators.required],
            heightCm: [null as number | null, [Validators.required, Validators.min(50), Validators.max(300)]],
            weightKg: [null as number | null, [Validators.required, Validators.min(20), Validators.max(500)]],
            hasFoodAllergies: [false, Validators.required],
            foodAllergiesDetails: [null as string | null],
            hasChronicConditions: [false, Validators.required],
            chronicConditionsDetails: [null as string | null],
            primaryGoal: [null as PrimaryGoal | null, Validators.required]
        },
        {
            validators: [
                this.requiredIfCheckedValidator('hasFoodAllergies', 'foodAllergiesDetails'),
                this.requiredIfCheckedValidator('hasChronicConditions', 'chronicConditionsDetails')
            ]
        }
    );

    optionalForm!: FormGroup;

    ngOnInit(): void {
        this.optionalForm = this.fb.group(
            {
                sleepQuality: [null],
                stressLevel: [null],
                studyLoad: [null],

                activityLevel: [null],
                exerciseFrequencyPerWeek: [null, [Validators.min(0), Validators.max(7)]],
                dietType: [null],
                mealRegularity: [null],
                hydrationLevel: [null],

                fitnessLevel: [null],
                hasPhysicalLimitations: [null],
                physicalLimitationsDetails: [null],

                preferredRoutineTime: [null],
                preferredContentType: [null],
                wellnessFocus: [null]
            },
            {
                validators: [this.requiredIfTrueValidator('hasPhysicalLimitations', 'physicalLimitationsDetails')]
            }
        );

        this.setupConditionalLogic();
        this.loadBaseHealthProfile();
        this.loadOptionalProfile();
    }

    get f() {
        return this.baseForm.controls;
    }

    get of() {
        return this.optionalForm.controls;
    }

    editBase(): void {
        this.errorMessage = null;
        this.successMessage = null;
        this.isEditingBase = true;
    }

    cancelBaseEdit(): void {
        this.errorMessage = null;
        this.successMessage = null;

        if (this.initialBaseValue) {
            this.baseForm.reset(this.initialBaseValue);
        }

        this.baseForm.markAsPristine();
        this.baseForm.markAsUntouched();
        this.baseForm.updateValueAndValidity();

        this.isEditingBase = false;
    }

    editOptional(): void {
        this.optionalErrorMessage = null;
        this.optionalSuccessMessage = null;
        this.isEditingOptional = true;
    }

    cancelOptionalEdit(): void {
        this.optionalErrorMessage = null;
        this.optionalSuccessMessage = null;

        if (this.initialOptionalValue) {
            this.optionalForm.reset(this.initialOptionalValue);
        }

        this.optionalForm.markAsPristine();
        this.optionalForm.markAsUntouched();
        this.optionalForm.updateValueAndValidity();

        this.isEditingOptional = false;
    }

    onBaseSubmit(): void {
        this.submitted = true;
        this.errorMessage = null;
        this.successMessage = null;

        this.baseForm.markAllAsTouched();
        this.baseForm.updateValueAndValidity();

        if (this.baseForm.invalid || !this.baseForm.dirty) {
            return;
        }

        const dto = this.buildDto();
        this.saving = true;

        this.healthProfileService.updateMyProfile(dto).subscribe({
            next: (updatedProfile) => {
                const profileToUse = updatedProfile ?? dto;

                this.patchForm(profileToUse);
                this.storeInitialBaseValue();

                this.successMessage = 'Base health profile updated successfully.';
                this.isEditingBase = false;
                this.saving = false;
            },
            error: (error) => {
                console.error('Failed to update base health profile', error);
                this.errorMessage = 'Failed to update base health profile.';
                this.saving = false;
            }
        });
    }

    onOptionalSubmit(): void {
        this.optionalErrorMessage = null;
        this.optionalSuccessMessage = null;

        this.optionalForm.markAllAsTouched();
        this.optionalForm.updateValueAndValidity();

        if (this.optionalForm.invalid || !this.optionalForm.dirty) {
            return;
        }

        const value = this.optionalForm.getRawValue() as OptionalHealthProfileDto;

        if (value.hasPhysicalLimitations !== true) {
            value.physicalLimitationsDetails = null;
        }

        this.saving = true;

        this.optionalHealthProfileService.updateMyOptionalProfile(value).subscribe({
            next: (updatedValue) => {
                const dtoToUse = updatedValue ?? value;

                this.optionalForm.patchValue({
                    sleepQuality: dtoToUse.sleepQuality ?? null,
                    stressLevel: dtoToUse.stressLevel ?? null,
                    studyLoad: dtoToUse.studyLoad ?? null,
                    activityLevel: dtoToUse.activityLevel ?? null,
                    exerciseFrequencyPerWeek: dtoToUse.exerciseFrequencyPerWeek ?? null,
                    dietType: dtoToUse.dietType ?? null,
                    mealRegularity: dtoToUse.mealRegularity ?? null,
                    hydrationLevel: dtoToUse.hydrationLevel ?? null,
                    fitnessLevel: dtoToUse.fitnessLevel ?? null,
                    hasPhysicalLimitations: dtoToUse.hasPhysicalLimitations ?? null,
                    physicalLimitationsDetails: dtoToUse.physicalLimitationsDetails ?? null,
                    preferredRoutineTime: dtoToUse.preferredRoutineTime ?? null,
                    preferredContentType: dtoToUse.preferredContentType ?? null,
                    wellnessFocus: dtoToUse.wellnessFocus ?? null
                });

                this.storeInitialOptionalValue();

                this.optionalSuccessMessage = 'Optional profile updated successfully.';
                this.isEditingOptional = false;
                this.saving = false;

                this.optionalSubmit.emit(dtoToUse);
            },
            error: (error) => {
                console.error('Failed to update optional health profile', error);
                this.optionalErrorMessage = 'Failed to update optional health profile.';
                this.saving = false;
            }
        });
    }

    loadOptionalProfile(): void {
        this.optionalHealthProfileService.getMyOptionalProfile().subscribe({
            next: (dto) => {
                this.optionalForm.patchValue({
                    sleepQuality: dto?.sleepQuality ?? null,
                    stressLevel: dto?.stressLevel ?? null,
                    studyLoad: dto?.studyLoad ?? null,
                    activityLevel: dto?.activityLevel ?? null,
                    exerciseFrequencyPerWeek: dto?.exerciseFrequencyPerWeek ?? null,
                    dietType: dto?.dietType ?? null,
                    mealRegularity: dto?.mealRegularity ?? null,
                    hydrationLevel: dto?.hydrationLevel ?? null,
                    fitnessLevel: dto?.fitnessLevel ?? null,
                    hasPhysicalLimitations: dto?.hasPhysicalLimitations ?? null,
                    physicalLimitationsDetails: dto?.physicalLimitationsDetails ?? null,
                    preferredRoutineTime: dto?.preferredRoutineTime ?? null,
                    preferredContentType: dto?.preferredContentType ?? null,
                    wellnessFocus: dto?.wellnessFocus ?? null
                });

                this.storeInitialOptionalValue();
            },
            error: (error) => {
                console.error('Failed to load optional health profile', error);
                this.optionalErrorMessage = 'Failed to load optional health profile.';
            }
        });
    }

    private loadBaseHealthProfile(): void {
        this.loading = true;
        this.errorMessage = null;

        this.healthProfileService.getMyProfile().subscribe({
            next: (profile) => {
                this.patchForm(profile);
                this.storeInitialBaseValue();
                this.loading = false;
            },
            error: (error) => {
                console.error('Failed to load health profile', error);
                this.errorMessage = 'Failed to load health profile.';
                this.loading = false;
            }
        });
    }

    private storeInitialBaseValue(): void {
        this.initialBaseValue = this.baseForm.getRawValue();
        this.baseForm.markAsPristine();
        this.baseForm.markAsUntouched();
        this.baseForm.updateValueAndValidity();
    }

    private storeInitialOptionalValue(): void {
        this.initialOptionalValue = this.optionalForm.getRawValue();
        this.optionalForm.markAsPristine();
        this.optionalForm.markAsUntouched();
        this.optionalForm.updateValueAndValidity();
    }

    private buildDto(): HealthProfileDTO {
        const raw = this.baseForm.getRawValue();

        return {
            id: (raw as any).id ?? '',
            dateOfBirth: raw.dateOfBirth,
            gender: raw.gender,
            heightCm: raw.heightCm,
            weightKg: raw.weightKg,
            hasFoodAllergies: !!raw.hasFoodAllergies,
            foodAllergiesDetails: raw.hasFoodAllergies ? raw.foodAllergiesDetails : null,
            hasChronicConditions: !!raw.hasChronicConditions,
            chronicConditionsDetails: raw.hasChronicConditions ? raw.chronicConditionsDetails : null,
            primaryGoal: raw.primaryGoal
        };
    }

    private patchForm(profile: HealthProfileDTO): void {
        this.baseForm.patchValue({
            dateOfBirth: this.toDateInputValue(profile.dateOfBirth),
            gender: profile.gender ?? null,
            heightCm: profile.heightCm ?? null,
            weightKg: profile.weightKg ?? null,
            hasFoodAllergies: profile.hasFoodAllergies ?? false,
            foodAllergiesDetails: profile.foodAllergiesDetails ?? null,
            hasChronicConditions: profile.hasChronicConditions ?? false,
            chronicConditionsDetails: profile.chronicConditionsDetails ?? null,
            primaryGoal: profile.primaryGoal ?? null
        });

        if (profile.hasFoodAllergies !== true) {
            this.baseForm.patchValue({ foodAllergiesDetails: null }, { emitEvent: false });
        }

        if (profile.hasChronicConditions !== true) {
            this.baseForm.patchValue({ chronicConditionsDetails: null }, { emitEvent: false });
        }
    }

    private setupConditionalLogic(): void {
        this.baseForm.get('hasFoodAllergies')?.valueChanges.subscribe((hasAllergies) => {
            const detailsControl = this.baseForm.get('foodAllergiesDetails');
            if (!hasAllergies) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({ emitEvent: false });
            this.baseForm.updateValueAndValidity({ emitEvent: false });
        });

        this.baseForm.get('hasChronicConditions')?.valueChanges.subscribe((hasConditions) => {
            const detailsControl = this.baseForm.get('chronicConditionsDetails');
            if (!hasConditions) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({ emitEvent: false });
            this.baseForm.updateValueAndValidity({ emitEvent: false });
        });

        this.optionalForm.get('hasPhysicalLimitations')?.valueChanges.subscribe((hasLimitations) => {
            const detailsControl = this.optionalForm.get('physicalLimitationsDetails');
            if (hasLimitations !== true) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({ emitEvent: false });
            this.optionalForm.updateValueAndValidity({ emitEvent: false });
        });
    }

    private requiredIfCheckedValidator(toggleControlName: string, detailsControlName: string): ValidatorFn {
        return (group: AbstractControl): ValidationErrors | null => {
            const toggleValue = group.get(toggleControlName)?.value;
            const detailsValue = group.get(detailsControlName)?.value;

            if (toggleValue === true && (!detailsValue || !detailsValue.toString().trim())) {
                group.get(detailsControlName)?.setErrors({ required: true });
                return { requiredIfChecked: true };
            }

            const control = group.get(detailsControlName);
            if (control?.hasError('required')) {
                const currentErrors = control.errors;
                if (currentErrors) {
                    delete currentErrors['required'];
                    control.setErrors(Object.keys(currentErrors).length ? currentErrors : null);
                }
            }

            return null;
        };
    }

    private requiredIfTrueValidator(toggleControlName: string, detailsControlName: string): ValidatorFn {
        return (group: AbstractControl): ValidationErrors | null => {
            const toggleValue = group.get(toggleControlName)?.value;
            const detailsValue = group.get(detailsControlName)?.value;

            if (toggleValue === true && (!detailsValue || !detailsValue.toString().trim())) {
                group.get(detailsControlName)?.setErrors({ required: true });
                return { requiredIfTrue: true };
            }

            const control = group.get(detailsControlName);
            if (control?.hasError('required')) {
                const currentErrors = control.errors;
                if (currentErrors) {
                    delete currentErrors['required'];
                    control.setErrors(Object.keys(currentErrors).length ? currentErrors : null);
                }
            }

            return null;
        };
    }

    private toDateInputValue(value: Date | string | null | undefined): string | null {
        if (!value) {
            return null;
        }

        if (value instanceof Date) {
            return value.toISOString().slice(0, 10);
        }

        return value.toString().slice(0, 10);
    }
}

import {Component, EventEmitter, inject, OnInit, Output} from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import {OptionalHealthProfileService} from '../../../shared/services/optional-health-profile.service';
import {HealthProfileService} from '../../../shared/services/health-profile.service';
import {Gender, HealthProfileDTO, PrimaryGoal} from '../../../shared/interfaces/health-profile';
import {TranslatePipe} from "@ngx-translate/core";
import {MatDivider} from "@angular/material/list";
import {
    CoffeeConsumption,
    ContentFrequency, DietType, ExerciseFrequency, MealsPerDay,
    OptionalHealthProfile, OptionalHealthProfileDto,
    PreferredContentType, ScreenTime, SleepQuality, SmokingHabit, SnackFrequency, StudyLoad, WaterIntake, YesNoOption
} from "../../../shared/interfaces/optional-health-profile";

@Component({
    selector: 'app-complete-health-profile-form',
    templateUrl: './complete-health-profile-form.html',
    styleUrls: ['./complete-health-profile-form.scss'],
    imports: [ReactiveFormsModule, TranslatePipe, MatDivider]
})
export class CompleteHealthProfileForm implements OnInit {
    @Output() optionalSubmit = new EventEmitter<OptionalHealthProfile>();

    private readonly fb = inject(FormBuilder);
    private readonly optionalHealthProfileService = inject(OptionalHealthProfileService);
    private readonly healthProfileService = inject(HealthProfileService);
    age: number | null = null;
    bmi: string | null = null;

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

    readonly sleepQualityOptions = Object.values(SleepQuality);
    readonly studyLoadOptions = Object.values(StudyLoad);
    readonly smokingOptions = Object.values(SmokingHabit);
    readonly coffeeOptions = Object.values(CoffeeConsumption);
    readonly screenTimeOptions = Object.values(ScreenTime);

    readonly exerciseOptions = Object.values(ExerciseFrequency);
    readonly mealsPerDayOptions = Object.values(MealsPerDay);
    readonly snackOptions = Object.values(SnackFrequency);
    readonly waterOptions = Object.values(WaterIntake);
    readonly dietTypeOptions = Object.values(DietType);

    readonly preferredContentOptions = Object.values(PreferredContentType);
    readonly frequencyOptions = Object.values(ContentFrequency);

    readonly yesNoOptions = [
        {value: true, label: 'yes'},
        {value: false, label: 'no'}
    ];

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
        this.optionalForm = this.fb.group({
            sleepQuality: [null as SleepQuality | null],
            studyLoad: [null as StudyLoad | null],
            smoking: [null as SmokingHabit | null],
            coffee: [null as CoffeeConsumption | null],
            screenTime: [null as ScreenTime | null],

            exercise: [null as ExerciseFrequency | null],
            mealsPerDay: [null as MealsPerDay | null],
            eatSnack: [null as SnackFrequency | null],
            water: [null as WaterIntake | null],
            dietType: [null as DietType | null],

            medication: [null as YesNoOption | null],
            medicationDetails: [null as string | null],
            surgeryHistory: [null as YesNoOption | null],
            surgeryDetails: [null as string | null],

            preferredContent: [null as PreferredContentType | null],
            frequency: [null as ContentFrequency | null],
            comments: [null as string | null]
        });

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

    get isMedicationYes(): boolean {
        return this.optionalForm.get('medication')?.value === true;
    }

    get isSurgeryHistoryYes(): boolean {
        return this.optionalForm.get('surgeryHistory')?.value === true;
    }

    getEnumKey(value: string | null | undefined, group: string): string {
        return value ? `complete.health.optional.${group}.options.${value}` : '-';
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
                this.age = updatedProfile.age ?? null;
                this.bmi = updatedProfile.bmi ?? null;
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

        if (value.medication !== true) {
            value.medicationDetails = null;
        }

        if (value.surgeryHistory !== true) {
            value.surgeryDetails = null;
        }

        this.saving = true;
        this.optionalHealthProfileService.updateMyOptionalProfile(value).subscribe({
            next: (updatedValue) => {
                const dtoToUse = updatedValue ?? value;

                this.optionalForm.patchValue({
                    sleepQuality: dtoToUse.sleepQuality ?? null,
                    studyLoad: dtoToUse.studyLoad ?? null,
                    smoking: dtoToUse.smoking ?? null,
                    coffee: dtoToUse.coffee ?? null,
                    screenTime: dtoToUse.screenTime ?? null,

                    exercise: dtoToUse.exercise ?? null,
                    mealsPerDay: dtoToUse.mealsPerDay ?? null,
                    eatSnack: dtoToUse.eatSnack ?? null,
                    water: dtoToUse.water ?? null,
                    dietType: dtoToUse.dietType ?? null,

                    medication: dtoToUse.medication ?? null,
                    medicationDetails: dtoToUse.medicationDetails ?? null,
                    surgeryHistory: dtoToUse.surgeryHistory ?? null,
                    surgeryDetails: dtoToUse.surgeryDetails ?? null,

                    preferredContent: dtoToUse.preferredContent ?? null,
                    frequency: dtoToUse.frequency ?? null,
                    comments: dtoToUse.comments ?? null
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
                    studyLoad: dto?.studyLoad ?? null,
                    smoking: dto?.smoking ?? null,
                    coffee: dto?.coffee ?? null,
                    screenTime: dto?.screenTime ?? null,

                    exercise: dto?.exercise ?? null,
                    mealsPerDay: dto?.mealsPerDay ?? null,
                    eatSnack: dto?.eatSnack ?? null,
                    water: dto?.water ?? null,
                    dietType: dto?.dietType ?? null,

                    medication: dto?.medication ?? null,
                    medicationDetails: dto?.medicationDetails ?? null,
                    surgeryHistory: dto?.surgeryHistory ?? null,
                    surgeryDetails: dto?.surgeryDetails ?? null,

                    preferredContent: dto?.preferredContent ?? null,
                    frequency: dto?.frequency ?? null,
                    comments: dto?.comments ?? null
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
                this.age = profile.age ?? null;
                this.bmi = profile.bmi ?? null;
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

        if (!profile.hasFoodAllergies) {
            this.baseForm.patchValue({foodAllergiesDetails: null}, {emitEvent: false});
        }

        if (!profile.hasChronicConditions) {
            this.baseForm.patchValue({chronicConditionsDetails: null}, {emitEvent: false});
        }
    }

    private setupConditionalLogic(): void {
        this.baseForm.get('hasFoodAllergies')?.valueChanges.subscribe((hasAllergies) => {
            const detailsControl = this.baseForm.get('foodAllergiesDetails');
            if (!hasAllergies) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({emitEvent: false});
            this.baseForm.updateValueAndValidity({emitEvent: false});
        });

        this.baseForm.get('hasChronicConditions')?.valueChanges.subscribe((hasConditions) => {
            const detailsControl = this.baseForm.get('chronicConditionsDetails');
            if (!hasConditions) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({emitEvent: false});
            this.baseForm.updateValueAndValidity({emitEvent: false});
        });

        this.optionalForm.get('medication')?.valueChanges.subscribe((value) => {
            const detailsControl = this.optionalForm.get('medicationDetails');
            if (value !== YesNoOption.YES) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({emitEvent: false});
            this.optionalForm.updateValueAndValidity({emitEvent: false});
        });

        this.optionalForm.get('surgeryHistory')?.valueChanges.subscribe((value) => {
            const detailsControl = this.optionalForm.get('surgeryDetails');
            if (value !== YesNoOption.YES) {
                detailsControl?.setValue(null);
            }
            detailsControl?.updateValueAndValidity({emitEvent: false});
            this.optionalForm.updateValueAndValidity({emitEvent: false});
        });
    }

    private requiredIfCheckedValidator(toggleControlName: string, detailsControlName: string): ValidatorFn {
        return (group: AbstractControl): ValidationErrors | null => {
            const toggleValue = group.get(toggleControlName)?.value;
            const detailsValue = group.get(detailsControlName)?.value;

            if (toggleValue === true && (!detailsValue || !detailsValue.toString().trim())) {
                group.get(detailsControlName)?.setErrors({required: true});
                return {requiredIfChecked: true};
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

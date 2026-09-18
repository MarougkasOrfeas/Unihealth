import {Component, inject, Input, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {HealthProfileService} from "../../../shared/services/health-profile.service";
import {Gender, HealthProfileDTO, PrimaryGoal} from "../../../shared/interfaces/health-profile";
import {Router} from "@angular/router";
import {AuthService} from "../../auth/auth.service";
import {TranslatePipe} from "@ngx-translate/core";
import {map} from "rxjs";

@Component({
    selector: 'app-health-profile-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
    templateUrl: './health-profile-form.html',
    styleUrl: './health-profile-form.scss'
})
export class HealthProfileFormComponent implements OnInit {

    private fb = inject(FormBuilder);
    private healthProfileService = inject(HealthProfileService);
    private router = inject(Router);
    private authService = inject(AuthService);

    @Input() editMode = false;

    loading = false;
    saving = false;
    submitted = false;
    errorMessage: string | null = null;
    successMessage: string | null = null;

    readonly genders = Object.values(Gender);
    readonly primaryGoals = Object.values(PrimaryGoal);

    form = this.fb.group({
        dateOfBirth: [null as string | null, Validators.required],
        gender: [null as Gender | null, Validators.required],
        heightCm: [null as number | null, [Validators.required, Validators.min(50), Validators.max(300)]],
        weightKg: [null as number | null, [Validators.required, Validators.min(20), Validators.max(500)]],
        hasFoodAllergies: [false, Validators.required],
        foodAllergiesDetails: [null as string | null],
        hasChronicConditions: [false, Validators.required],
        chronicConditionsDetails: [null as string | null],
        primaryGoal: [null as PrimaryGoal | null, Validators.required]
    });

    ngOnInit(): void {
        this.registerConditionalLogic();

        if (this.editMode) {
            this.loadMyProfile();
        }
    }

    private registerConditionalLogic(): void {
        this.form.get('hasFoodAllergies')?.valueChanges.subscribe(value => {
            const control = this.form.get('foodAllergiesDetails');
            if (value) {
                control?.addValidators([Validators.required]);
            } else {
                control?.removeValidators([Validators.required]);
                control?.setValue(null);
            }
            control?.updateValueAndValidity();
        });

        this.form.get('hasChronicConditions')?.valueChanges.subscribe(value => {
            const control = this.form.get('chronicConditionsDetails');
            if (value) {
                control?.addValidators([Validators.required]);
            } else {
                control?.removeValidators([Validators.required]);
                control?.setValue(null);
            }
            control?.updateValueAndValidity();
        });
    }

    private loadMyProfile(): void {
        this.loading = true;
        this.errorMessage = null;

        this.healthProfileService.getMyProfile().subscribe({
            next: profile => {
                this.patchForm(profile);
                this.loading = false;
            },
            error: () => {
                this.errorMessage = 'Failed to load health profile.';
                this.loading = false;
            }
        });
    }

    private patchForm(profile: HealthProfileDTO): void {
        this.form.patchValue({
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
    }

    submit(): void {
        this.submitted = true;
        this.errorMessage = null;
        this.successMessage = null;

        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const dto = this.buildDto();
        this.saving = true;

        const request$ = this.editMode
            ? this.healthProfileService.updateMyProfile(dto).pipe(map(() => void 0))
            : this.healthProfileService.completeProfile(dto);

        request$.subscribe({
            next: () => {
                this.successMessage = this.editMode
                    ? 'Health profile updated successfully.'
                    : 'Health profile completed successfully.';
                this.saving = false;
                if (!this.editMode) {
                    this.authService.setHealthProfileCompleted(true);
                }
                this.router.navigate(['']);
            },
            error: () => {
                this.errorMessage = this.editMode
                    ? 'Failed to update health profile.'
                    : 'Failed to complete health profile.';
                this.saving = false;
            }
        });
    }

    private buildDto(): HealthProfileDTO {
        const raw = this.form.getRawValue();

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

    private toDateInputValue(value: Date | string | null | undefined): string | null {
        if (!value) {
            return null;
        }

        if (value instanceof Date) {
            return value.toISOString().slice(0, 10);
        }

        return value.toString().slice(0, 10);
    }

    get f() {
        return this.form.controls;
    }

    /**
     * Lexicon key for an enum value, e.g. MALE -> complete.health.optional.gender.options.MALE.
     *
     * Deliberately the same namespace the optional form uses: both screens offer the same Gender
     * and PrimaryGoal choices, so sharing the keys keeps one wording instead of two that drift.
     */
    getEnumKey(value: string | null | undefined, group: string): string {
        return value ? `complete.health.optional.${group}.options.${value}` : '-';
    }

}

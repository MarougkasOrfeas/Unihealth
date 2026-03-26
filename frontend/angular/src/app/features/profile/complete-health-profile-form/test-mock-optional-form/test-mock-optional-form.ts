import {Component, inject} from '@angular/core';
import {
    FormBuilder,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';
import {JsonPipe} from '@angular/common';
import {MatDivider} from "@angular/material/list";

@Component({
    selector: 'app-test-mock-optional-form',
    standalone: true,
    imports: [ReactiveFormsModule, JsonPipe, MatDivider],
    templateUrl: './test-mock-optional-form.html',
    styleUrls: ['./test-mock-optional-form.scss']
})
export class TestMockOptionalForm {
    private readonly fb = inject(FormBuilder);

    readonly sleepQualityOptions = [
        'Λιγότερες από 6 ώρες',
        'Περίπου 6 ώρες',
        'Περίπου 8 ώρες',
        'Περισσότερες απο 8 ώρες'
    ];

    readonly studyLoadOptions = [
        'Λιγότερο απο 1 ώρα την μέρα',
        '1-2 ώρες την μέρα',
        '2-4 ώρες την μέρα',
        'Περισσότερες απο 4 ώρες την μέρα'
    ];

    readonly smokingOptions = [
        'Όχι',
        'Περιστασιακά',
        'Καθημερινά'
    ];

    readonly coffeeOptions = [
        'Δεν καταναλώνω',
        '1 φλιτζάνι',
        '2–3 φλιτζάνια',
        '4+ φλιτζάνια'
    ];

    readonly screenTimeOptions = [
        'Λιγότερο από 2 ώρες',
        '2–4 ώρες',
        '4–6 ώρες',
        'Περισσότερες από 6 ώρες'
    ];

    readonly exerciseOptions = [
        'Καθόλου',
        '1-2 φορές',
        '3-5 φορές',
        '5-7 φορές'
    ];


    readonly mealsPerDayOptions = [
        '1-2',
        '3',
        '4-5',
        '> 5'
    ];

    readonly snackOptions = [
        'Όχι',
        'Μερικές φορές',
        'Καθημερινά'
    ];

    readonly waterOptions = [
        '1-3 (~ 1L)',
        '4-6 (~ 2L)',
        '7-9 (~ 3L)',
        ' περισσότερο απο 9'
    ];

    readonly dietTypeOptions = [
        'Παμφάγος',
        'Χορτοφάγος',
        'Vegan',
        'Pescatarian',
        'Γλουτένη',
        'Άλλο'
    ];

    readonly preferredContentOptions = [
        'Άρθρα',
        'Σύντομες συμβουλές',
        'Μικτό'
    ];

    readonly frequencyOptions = [
        'Καθημερινά',
        'Μερικές φορές την εβδομάδα',
        'Σπάνια'
    ];

    readonly yesNoOptions = [
        'Όχι',
        'Ναι'
    ];

    readonly form = this.fb.group({
        sleepQuality: [null as string | null],
        studyLoad: [null as string | null],
        smoking: [null as string | null],
        coffee: [null as string | null],
        screenTime: [null as string | null],
        exercise: [null as string | null],
        mealsPerDay: [null as string | null],
        eatSnack: [null as string | null],
        water: [null as string | null],
        dietType: [null as string | null],
        preferredContent: [null as string | null],
        frequency: [null as string | null],
        comments: [null as string | null],

        medication: [null as string | null],
        medicationDetails: [null as string | null],
        surgeryHistory: [null as string | null],
        surgeryDetails: [null as string | null]
    });

    get f() {
        return this.form.controls;
    }

    onSubmit(): void {
        this.form.markAllAsTouched();

        if (this.form.invalid) {
            return;
        }

        console.log('Mock optional form value:', this.form.getRawValue());
    }
}

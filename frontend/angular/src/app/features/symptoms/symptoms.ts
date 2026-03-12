import {Component, computed, effect, inject, signal} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {MatIconModule} from "@angular/material/icon";
import {SymptomService} from "../../shared/services/symptom.service";
import {SymptomItem} from "../../shared/interfaces/symptom-item";
import {RouterLink} from "@angular/router";

@Component({
    selector: 'app-symptoms',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatIconModule,
        RouterLink
    ],
    templateUrl: './symptoms.html',
    styleUrl: './symptoms.scss'
})
export class Symptoms {

    private symptomService = inject(SymptomService);

    searchTerm = signal('');
    symptoms = signal<SymptomItem[]>([]);
    loading = signal(false);

    alphabet: string[] = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

    constructor() {
        this.loadContent();

        effect(() => {
            const search = this.searchTerm();
            this.loadContent(search);
        });
    }

    groupedSymptoms = computed(() => {
        const grouped: Record<string, SymptomItem[]> = {};

        for (const letter of this.alphabet) {
            grouped[letter] = [];
        }

        for (const item of this.symptoms()) {
            const letter = item.startingLetter?.toUpperCase();
            if (letter && grouped[letter]) {
                grouped[letter].push(item);
            }
        }

        return grouped;
    });

    availableLetters = computed(() =>
        this.alphabet.filter(letter => this.groupedSymptoms()[letter]?.length > 0)
    );

    getSymptomsByLetter(letter: string): SymptomItem[] {
        return this.groupedSymptoms()[letter] ?? [];
    }

    scrollToLetter(letter: string): void {
        const element = document.getElementById(`letter-${letter}`);
        if (element) {
            element.scrollIntoView({behavior: 'smooth', block: 'start'});
        }
    }

    private loadContent(search = ''): void {
        this.loading.set(true);

        this.symptomService.findContent(search).subscribe({
            next: data => this.symptoms.set(data),
            error: err => {
                console.error('Error loading symptoms', err);
                this.symptoms.set([]);
            },
            complete: () => this.loading.set(false)
        });
    }
}

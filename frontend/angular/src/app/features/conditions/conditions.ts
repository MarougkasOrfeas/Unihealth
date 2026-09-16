import {Component, computed, effect, inject, signal} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {MatIconModule} from "@angular/material/icon";
import {RouterLink} from "@angular/router";
import {ConditionService} from "../../shared/services/condition.service";
import {ConditionItem} from "../../shared/interfaces/condition-item";
import {DataSources} from "../../shared/components/data-sources/data-sources";

/**
 * The conditions A-Z, built from the causes listed on the symptom pages.
 *
 * <p>Deliberately mirrors the symptoms A-Z, including the letter navigation, so the two read as one
 * reference section.
 */
@Component({
    selector: 'app-conditions',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatIconModule,
        RouterLink,
        DataSources
    ],
    templateUrl: './conditions.html',
    styleUrl: './conditions.scss'
})
export class Conditions {

    private conditionService = inject(ConditionService);

    searchTerm = signal('');
    conditions = signal<ConditionItem[]>([]);
    loading = signal(false);

    alphabet: string[] = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

    constructor() {
        this.loadContent();

        effect(() => {
            const search = this.searchTerm();
            this.loadContent(search);
        });
    }

    groupedConditions = computed(() => {
        const grouped: Record<string, ConditionItem[]> = {};

        for (const letter of this.alphabet) {
            grouped[letter] = [];
        }

        for (const item of this.conditions()) {
            const letter = item.startingLetter?.toUpperCase();
            if (letter && grouped[letter]) {
                grouped[letter].push(item);
            }
        }

        return grouped;
    });

    availableLetters = computed(() =>
        this.alphabet.filter(letter => this.groupedConditions()[letter]?.length > 0)
    );

    getConditionsByLetter(letter: string): ConditionItem[] {
        return this.groupedConditions()[letter] ?? [];
    }

    scrollToLetter(letter: string): void {
        const element = document.getElementById(`letter-${letter}`);
        if (element) {
            element.scrollIntoView({behavior: 'smooth', block: 'start'});
        }
    }

    private loadContent(search = ''): void {
        this.loading.set(true);

        this.conditionService.findContent(search).subscribe({
            next: data => this.conditions.set(data),
            error: err => {
                console.error('Error loading conditions', err);
                this.conditions.set([]);
            },
            complete: () => this.loading.set(false)
        });
    }
}

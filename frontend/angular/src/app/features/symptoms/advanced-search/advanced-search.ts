import {Component, computed, inject, signal} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {MatIconModule} from "@angular/material/icon";
import {RouterLink} from "@angular/router";
import {SymptomService} from "../../../shared/services/symptom.service";
import {SymptomItem} from "../../../shared/interfaces/symptom-item";
import {SymptomItemDetail} from "../../../shared/interfaces/symptom-item-detail";
import {SymptomFactor} from "../../../shared/interfaces/symptom-factor";
import {PossibleCause} from "../../../shared/interfaces/possible-cause";

/**
 * Narrow a symptom down by its details and see what the NHS lists as possible causes.
 *
 * <p>Ranking happens on the server; this component only shows what came back, including which of
 * the reader's ticks matched. Causes that matched nothing are kept in a separate group rather than
 * hidden, because the source lists them.
 */
@Component({
    selector: 'app-advanced-search',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatIconModule,
        RouterLink
    ],
    templateUrl: './advanced-search.html',
    styleUrl: './advanced-search.scss'
})
export class AdvancedSearch {

    private symptomService = inject(SymptomService);

    /** Symptoms that have a causes table; the rest have nothing to narrow down. */
    searchableSymptoms = signal<SymptomItem[]>([]);
    symptomQuery = signal('');
    selected = signal<SymptomItem | null>(null);
    detail = signal<SymptomItemDetail | null>(null);

    factors = signal<SymptomFactor[]>([]);
    selectedCodes = signal<string[]>([]);
    causes = signal<PossibleCause[]>([]);

    loadingSymptoms = signal(false);
    loadingCauses = signal(false);

    constructor() {
        this.loadSearchableSymptoms();
    }

    suggestions = computed(() => {
        const query = this.symptomQuery().trim().toLowerCase();
        const all = this.searchableSymptoms();
        if (!query) {
            return all;
        }
        return all.filter(s =>
            s.title.toLowerCase().includes(query) ||
            (s.synonyms ?? '').toLowerCase().includes(query));
    });

    matched = computed(() => this.causes().filter(c => c.matchedCount > 0));
    unmatched = computed(() => this.causes().filter(c => c.matchedCount === 0));

    hasSelection = computed(() => this.selectedCodes().length > 0);

    selectSymptom(symptom: SymptomItem): void {
        this.selected.set(symptom);
        this.symptomQuery.set('');
        this.selectedCodes.set([]);
        this.factors.set([]);
        this.causes.set([]);

        this.symptomService.findBySlug(symptom.slug).subscribe({
            next: data => this.detail.set(data),
            error: () => this.detail.set(null)
        });

        this.symptomService.findFactors(symptom.slug).subscribe({
            next: data => this.factors.set(data),
            error: () => this.factors.set([])
        });

        this.loadCauses();
    }

    clearSymptom(): void {
        this.selected.set(null);
        this.detail.set(null);
        this.factors.set([]);
        this.selectedCodes.set([]);
        this.causes.set([]);
    }

    toggleFactor(code: string): void {
        const current = this.selectedCodes();
        this.selectedCodes.set(
            current.includes(code) ? current.filter(c => c !== code) : [...current, code]);
        this.loadCauses();
    }

    isSelected(code: string): boolean {
        return this.selectedCodes().includes(code);
    }

    clearFactors(): void {
        this.selectedCodes.set([]);
        this.loadCauses();
    }

    /** How much of this cause's own description the reader recognised, as a percentage. */
    matchPercent(cause: PossibleCause): number {
        return Math.round(cause.score * 100);
    }

    isMatched(cause: PossibleCause, code: string): boolean {
        return cause.matchedFactorCodes.includes(code);
    }

    asLines(text?: string | null): string[] {
        if (!text) return [];
        return text.split('\n').map(line => line.trim()).filter(line => !!line);
    }

    private loadCauses(): void {
        const symptom = this.selected();
        if (!symptom) {
            return;
        }

        this.loadingCauses.set(true);
        this.symptomService.findPossibleCauses(symptom.slug, this.selectedCodes()).subscribe({
            next: data => this.causes.set(data),
            error: err => {
                console.error('Error loading possible causes', err);
                this.causes.set([]);
            },
            complete: () => this.loadingCauses.set(false)
        });
    }

    private loadSearchableSymptoms(): void {
        this.loadingSymptoms.set(true);
        this.symptomService.findContent().subscribe({
            next: data => this.searchableSymptoms.set(data.filter(s => s.hasFactors)),
            error: err => {
                console.error('Error loading symptoms', err);
                this.searchableSymptoms.set([]);
            },
            complete: () => this.loadingSymptoms.set(false)
        });
    }
}

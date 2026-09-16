import {Component, inject, signal} from "@angular/core";
import {ActivatedRoute, RouterLink} from "@angular/router";
import {SymptomItemDetail} from "../../../shared/interfaces/symptom-item-detail";
import {SymptomService} from "../../../shared/services/symptom.service";
import {CommonModule} from "@angular/common";
import {MatIconModule} from "@angular/material/icon";
import {DataSources} from "../../../shared/components/data-sources/data-sources";

@Component({
    selector: 'app-symptom-details',
    standalone: true,
    imports: [
        CommonModule,
        MatIconModule,
        RouterLink,
        DataSources
    ],
    templateUrl: './symptom-details.html',
    styleUrl: './symptom-details.scss'
})
export class SymptomDetails {

    private route = inject(ActivatedRoute);
    private symptomService = inject(SymptomService);

    symptom = signal<SymptomItemDetail | null>(null);
    loading = signal(true);

    constructor() {
        const slug = this.route.snapshot.paramMap.get('slug');
        if (slug) {
            this.loadSymptom(slug);
        }
    }

    asLines(text?: string | null): string[] {
        if (!text) return [];
        return text
            .split('\n')
            .map(line => line.trim())
            .filter(line => !!line);
    }

    private loadSymptom(slug: string): void {
        this.loading.set(true);

        this.symptomService.findBySlug(slug).subscribe({
            next: data => this.symptom.set(data),
            error: err => {
                console.error('Error loading symptom detail', err);
                this.symptom.set(null);
            },
            complete: () => this.loading.set(false)
        });
    }
}

import {Component, inject, signal} from "@angular/core";
import {ActivatedRoute, RouterLink} from "@angular/router";
import {CommonModule} from "@angular/common";
import {MatIconModule} from "@angular/material/icon";
import {ConditionService} from "../../../shared/services/condition.service";
import {ConditionDetail} from "../../../shared/interfaces/condition-detail";
import {DataSources} from "../../../shared/components/data-sources/data-sources";

/**
 * A condition page.
 *
 * <p>There is no article here by design: the NHS robots.txt disallows its condition pages, so they
 * are never ingested. What the application can honestly offer is the route in - the symptoms whose
 * causes table leads here - and a link out to the publisher.
 */
@Component({
    selector: 'app-condition-details',
    standalone: true,
    imports: [
        CommonModule,
        MatIconModule,
        RouterLink,
        DataSources
    ],
    templateUrl: './condition-details.html',
    styleUrl: './condition-details.scss'
})
export class ConditionDetails {

    private route = inject(ActivatedRoute);
    private conditionService = inject(ConditionService);

    condition = signal<ConditionDetail | null>(null);
    loading = signal(true);

    constructor() {
        const slug = this.route.snapshot.paramMap.get('slug');
        if (slug) {
            this.loadCondition(slug);
        }
    }

    private loadCondition(slug: string): void {
        this.loading.set(true);

        this.conditionService.findBySlug(slug).subscribe({
            next: data => this.condition.set(data),
            error: err => {
                console.error('Error loading condition detail', err);
                this.condition.set(null);
            },
            complete: () => this.loading.set(false)
        });
    }
}

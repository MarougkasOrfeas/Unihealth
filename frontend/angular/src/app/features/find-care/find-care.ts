import {Component, computed, OnDestroy, signal} from '@angular/core';
import {ActivatedRoute, ParamMap, RouterLink} from '@angular/router';
import {Subscription} from 'rxjs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {PageEvent} from '@angular/material/paginator';
import {Sort} from '@angular/material/sort';
import {Table} from '../../shared/components/table/table';
import {TableColumn} from '../../shared/interfaces/table-column';
import {CareResultRow, FIND_CARE_MOCK_RESULTS} from './find-care.mock';

type CareCard = {
    id: string;
    title: string;
    description: string;
    icon: string;
    route: string[];
};

type DoctorSpecialty = {
    id: string;
    title: string;
};

@Component({
    selector: 'app-find-care',
    standalone: true,
    imports: [
        RouterLink,
        MatButtonModule,
        MatIconModule,
        MatMenuModule,
        Table,
    ],
    templateUrl: './find-care.html',
    styleUrl: './find-care.scss',
})
export class FindCare implements OnDestroy {
    readonly cards: CareCard[] = [
        {
            id: 'hospitals',
            title: 'Νοσοκομεία',
            description: 'Βρες νοσοκομεία, διαθέσιμες υπηρεσίες και βασικές πληροφορίες πρόσβασης.',
            icon: 'local_hospital',
            route: ['/find-doctor', 'hospitals'],
        },
        {
            id: 'pharmacies',
            title: 'Φαρμακεία',
            description: 'Αναζήτησε φαρμακεία και χρήσιμες πληροφορίες για την περιοχή σου.',
            icon: 'local_pharmacy',
            route: ['/find-doctor', 'pharmacies'],
        },
        {
            id: 'dentists',
            title: 'Οδοντίατροι',
            description: 'Βρες οδοντιατρικές υπηρεσίες και διαθέσιμες επιλογές φροντίδας.',
            icon: 'dentistry',
            route: ['/find-doctor', 'dentists'],
        },
    ];

    readonly specialties: DoctorSpecialty[] = [
        {id: 'general-practitioner', title: 'Γενικός Ιατρός'},
        {id: 'cardiologist', title: 'Καρδιολόγος'},
        {id: 'dermatologist', title: 'Δερματολόγος'},
        {id: 'orthopedic', title: 'Ορθοπαιδικός'},
        {id: 'pediatrician', title: 'Παιδίατρος'},
        {id: 'psychiatrist', title: 'Ψυχίατρος'},
    ];

    readonly selectedTitle = signal<string | null>(null);
    readonly resultRows = signal<CareResultRow[]>([]);
    readonly resultColumns: TableColumn<CareResultRow>[] = [
        {key: 'name', header: 'Όνομα', sortable: true, filterable: true, filterSearchable: true},
        {key: 'type', header: 'Είδος', sortable: true, filterable: true, filterSearchable: true},
        {key: 'place', header: 'Περιοχή', sortable: true, filterable: true, filterSearchable: true},
        {key: 'services', header: 'Υπηρεσίες', sortable: true, filterable: true, filterSearchable: true},
        {key: 'contact', header: 'Επικοινωνία', sortable: true},
        {key: 'availability', header: 'Διαθεσιμότητα', sortable: true, filterable: true, filterSearchable: true},
    ];
    readonly totalElements = computed(() => this.resultRows().length);
    readonly currentPageSize = 10;
    readonly isLoading = false;
    readonly facetOptions = computed<Record<string, string[]>>(() => ({
        name: this.uniqueValues('name'),
        type: this.uniqueValues('type'),
        place: this.uniqueValues('place'),
        services: this.uniqueValues('services'),
        availability: this.uniqueValues('availability'),
    }));
    readonly facetSelection: Record<string, Set<string>> = {
        name: new Set<string>(),
        type: new Set<string>(),
        place: new Set<string>(),
        services: new Set<string>(),
        availability: new Set<string>(),
    };
    private readonly routeSubscription: Subscription;

    constructor(private readonly route: ActivatedRoute) {
        this.routeSubscription = this.route.paramMap.subscribe((params) => {
            this.selectedTitle.set(this.resolveSelectionTitle(params));
            this.resultRows.set(this.resolveRows(params));
        });
    }

    ngOnDestroy(): void {
        this.routeSubscription.unsubscribe();
    }

    onPageChange(event: PageEvent): void {
        console.log('find care page changed', event);
    }

    onSortChange(sort: Sort): void {
        console.log('find care sort changed', sort);
    }

    onFacetOpened(key: string): void {
        console.log('find care facet opened', key);
    }

    onFacetChange(event: { key: string; selection: Set<string> }): void {
        console.log('find care facet changed', event.key, event.selection);
    }

    private resolveSelectionTitle(params: ParamMap): string | null {
        const specialty = params.get('specialty');
        if (specialty) {
            const selectedSpecialty = this.specialties.find(item => item.id === specialty);
            return selectedSpecialty ? `Γιατροί - ${selectedSpecialty.title}` : 'Γιατροί';
        }

        const category = params.get('category');
        if (!category) {
            return null;
        }

        if (category === 'doctors') {
            return 'Γιατροί';
        }

        return this.cards.find(card => card.id === category)?.title ?? null;
    }

    private resolveRows(params: ParamMap): CareResultRow[] {
        const specialty = params.get('specialty');
        if (specialty) {
            return FIND_CARE_MOCK_RESULTS[`doctors:${specialty}`] ?? FIND_CARE_MOCK_RESULTS['doctors'];
        }

        const category = params.get('category');
        if (!category) {
            return [];
        }

        return FIND_CARE_MOCK_RESULTS[category] ?? [];
    }

    private uniqueValues(key: keyof CareResultRow): string[] {
        return [...new Set(this.resultRows().map(row => row[key]).filter(Boolean))];
    }
}

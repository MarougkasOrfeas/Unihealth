import {Component, OnDestroy, signal} from '@angular/core';
import {ActivatedRoute, ParamMap, RouterLink} from '@angular/router';
import {Subscription} from 'rxjs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MockTable} from '../../shared/components/mock-table/mock-table';
import {MockTableColumn} from '../../shared/components/mock-table/mock-table-column';
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
        MockTable,
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
    readonly resultColumns: MockTableColumn<CareResultRow>[] = [
        {key: 'name', header: 'Όνομα', sortable: true},
        {key: 'type', header: 'Είδος', sortable: true, filterable: true, filterSearchable: false},
        {key: 'place', header: 'Περιοχή', sortable: true, filterable: true},
        {
            key: 'services',
            header: 'Υπηρεσίες',
            sortable: true,
            filterable: true,
            // One cell lists several services, so split it — otherwise the facet offers one
            // option per row, which filters nothing useful.
            facetValues: (row) => row.services.split(',').map((service) => service.trim()),
        },
        {key: 'contact', header: 'Επικοινωνία'},
        {key: 'availability', header: 'Διαθεσιμότητα', sortable: true, filterable: true},
    ];
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
}

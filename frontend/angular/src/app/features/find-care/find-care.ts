import {Component, OnDestroy, signal} from '@angular/core';
import {ActivatedRoute, ParamMap, RouterLink} from '@angular/router';
import {Subscription} from 'rxjs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';

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

type ResultColumn = {
    key: string;
    label: string;
    helper: string;
};

@Component({
    selector: 'app-find-care',
    standalone: true,
    imports: [
        RouterLink,
        MatButtonModule,
        MatIconModule,
        MatMenuModule,
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
    readonly resultColumns: ResultColumn[] = [
        {
            key: 'name',
            label: 'Name',
            helper: 'Provider, service, clinic, or facility name',
        },
        {
            key: 'type',
            label: 'Type',
            helper: 'Hospital, pharmacy, dentist, doctor, or other care type',
        },
        {
            key: 'place',
            label: 'Place',
            helper: 'City, area, or full address',
        },
        {
            key: 'services',
            label: 'Services',
            helper: 'Main service, specialty, or category match',
        },
        {
            key: 'contact',
            label: 'Contact',
            helper: 'Phone, email, or booking link',
        },
        {
            key: 'availability',
            label: 'Availability',
            helper: 'Open hours, appointment status, or emergency availability',
        },
    ];
    private readonly routeSubscription: Subscription;

    constructor(private readonly route: ActivatedRoute) {
        this.routeSubscription = this.route.paramMap.subscribe((params) => {
            this.selectedTitle.set(this.resolveSelectionTitle(params));
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
}

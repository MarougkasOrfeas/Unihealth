import {Component, computed, signal} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {MatDialogModule, MatDialogRef} from "@angular/material/dialog";
import {FormsModule} from "@angular/forms";
import {CommonModule} from "@angular/common";
import {UnihealthAssistantService} from "../../../shared/services/unihealth-assistant.service";
import {UnihealthAssistantContent, UnihealthAssistantItem} from "../../../shared/interfaces/unihealth-assistant";

@Component({
    selector: 'app-university-health-assistant',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatDialogModule,
        MatIconModule
    ],
    templateUrl: './unihealth-assistant.html',
    styleUrl: './unihealth-assistant.scss'
})
export class UniHealthAssistantComponent {

    searchTerm = signal('');
    activeSection = signal<'faq' | 'services' | 'emergency' | 'clinics'>('faq');
    content = signal<UnihealthAssistantContent>({
        faq: [],
        services: [],
        emergency: [],
        clinics: []
    });

    constructor(
        private dialogRef: MatDialogRef<UniHealthAssistantComponent>,
        private service: UnihealthAssistantService,
    ) {
        this.loadContent();
    }

    filteredFaq = computed(() => this.filterItems(this.content().faq));
    filteredServices = computed(() => this.filterItems(this.content().services));
    filteredEmergencyContacts = computed(() => this.filterItems(this.content().emergency));
    filteredClinics = computed(() => this.filterItems(this.content().clinics));

    setSection(section: 'faq' | 'services' | 'emergency' | 'clinics'): void {
        this.activeSection.set(section);
    }

    close(): void {
        this.dialogRef.close();
    }

    toggleExpanded(item: UnihealthAssistantItem): void {
        item.expanded = !item.expanded;
    }

    private loadContent(): void {
        this.service.getContent().subscribe(content => {
            this.content.set({
                faq: content.faq.map(item => ({...item, expanded: false})),
                services: content.services.map(item => ({...item, expanded: false})),
                emergency: content.emergency.map(item => ({...item, expanded: false})),
                clinics: content.clinics.map(item => ({...item, expanded: false}))
            });
        });
    }

    private filterItems(items: UnihealthAssistantItem[]): UnihealthAssistantItem[] {
        const term = this.searchTerm().trim().toLowerCase();
        if (!term) return items;

        return items.filter(item =>
            item.title.toLowerCase().includes(term) ||
            (item.brief?.toLowerCase().includes(term) ?? false) ||
            (item.content?.toLowerCase().includes(term) ?? false)
        );
    }

}

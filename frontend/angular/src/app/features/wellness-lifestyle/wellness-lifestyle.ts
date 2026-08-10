import {Component, computed, inject, signal} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {WELLNESS_COLLECTIONS, WellnessItem, WellnessPageKey} from './wellness-lifestyle.mock';
import {WellnessDetailDialog} from './wellness-detail-dialog/wellness-detail.dialog';

@Component({
    selector: 'app-wellness-lifestyle',
    standalone: true,
    imports: [MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
    templateUrl: './wellness-lifestyle.html',
    styleUrl: './wellness-lifestyle.scss',
})
export class WellnessLifestyle {
    private readonly route = inject(ActivatedRoute);

    readonly pageKey = signal<WellnessPageKey>(
        (this.route.snapshot.data['wellnessPage'] as WellnessPageKey) ?? 'recipes'
    );
    readonly selectedCategory = signal('All');
    readonly searchTerm = signal('');
    readonly collection = computed(() => WELLNESS_COLLECTIONS[this.pageKey()]);
    readonly featuredItem = computed(() => this.collection().items[0]);
    readonly filteredItems = computed(() => {
        const term = this.searchTerm().trim().toLowerCase();
        const category = this.selectedCategory();

        return this.collection().items.filter(item => {
            const categoryMatches = category === 'All' || item.category === category;
            const textMatches = !term || [
                item.title,
                item.category,
                item.summary,
                ...item.tags,
            ].some(value => value.toLowerCase().includes(term));

            return categoryMatches && textMatches;
        });
    });

    constructor(private readonly dialog: MatDialog) {
    }

    selectCategory(category: string): void {
        this.selectedCategory.set(category);
    }

    searchItems(event: Event): void {
        this.searchTerm.set((event.target as HTMLInputElement).value);
    }

    clearFilters(): void {
        this.selectedCategory.set('All');
        this.searchTerm.set('');
    }

    openItem(item: WellnessItem): void {
        this.dialog.open(WellnessDetailDialog, {
            width: '720px',
            maxWidth: '94vw',
            data: item,
        });
    }
}

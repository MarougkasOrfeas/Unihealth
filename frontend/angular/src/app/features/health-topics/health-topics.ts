import {Component, computed, signal} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {
    HEALTH_TOPIC_CATEGORIES,
    HEALTH_TOPICS,
    HealthTopic,
    HealthTopicCategory,
} from './health-topics.mock';
import {HealthTopicDialog} from './health-topic-dialog/health-topic.dialog';

@Component({
    selector: 'app-health-topics',
    standalone: true,
    imports: [
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatTooltipModule,
    ],
    templateUrl: './health-topics.html',
    styleUrl: './health-topics.scss',
})
export class HealthTopics {
    readonly categories = HEALTH_TOPIC_CATEGORIES;
    readonly selectedCategory = signal<HealthTopicCategory | 'All'>('All');
    readonly searchTerm = signal('');
    readonly topics = signal(HEALTH_TOPICS);
    readonly featuredTopic = computed(() => this.topics()[0]);

    readonly filteredTopics = computed(() => {
        const category = this.selectedCategory();
        const term = this.searchTerm().trim().toLowerCase();

        return this.topics().filter(topic => {
            const categoryMatches = category === 'All' || topic.category === category;
            const textMatches =
                !term ||
                [
                    topic.title,
                    topic.category,
                    topic.summary,
                    ...topic.tags,
                ].some(value => value.toLowerCase().includes(term));

            return categoryMatches && textMatches;
        });
    });

    readonly topicCount = computed(() => this.filteredTopics().length);
    readonly popularTags = computed(() =>
        Array.from(new Set(this.topics().flatMap(topic => topic.tags))).slice(0, 8)
    );

    constructor(private readonly dialog: MatDialog) {
    }

    selectCategory(category: HealthTopicCategory | 'All'): void {
        this.selectedCategory.set(category);
    }

    searchTopics(event: Event): void {
        this.searchTerm.set((event.target as HTMLInputElement).value);
    }

    applyTag(tag: string): void {
        this.searchTerm.set(tag);
    }

    clearFilters(): void {
        this.selectedCategory.set('All');
        this.searchTerm.set('');
    }

    openTopic(topic: HealthTopic): void {
        this.dialog.open(HealthTopicDialog, {
            width: '760px',
            maxWidth: '94vw',
            panelClass: 'health-topic-dialog-panel',
            data: topic,
        });
    }
}

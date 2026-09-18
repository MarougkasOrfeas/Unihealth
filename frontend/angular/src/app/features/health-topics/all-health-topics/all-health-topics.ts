import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {ActivatedRoute} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {finalize} from 'rxjs';
import {
    ActiveFilter,
    ActiveFiltersToolbar,
} from '../../../shared/components/active-filters-toolbar/active-filters-toolbar';
import {openHealthTopicDialog} from '../health-topic-dialog/health-topic.dialog';
import {UsageTrackingService} from '../../../core/services/usage-tracking.service';
import {HealthTopicFavouritesService} from '../health-topic-favourites.service';
import {
    HEALTH_TOPIC_CATEGORIES,
    HealthTopic,
    HealthTopicCategory,
    HealthTopicView,
} from '../health-topics.model';
import {HealthTopicsService} from '../health-topics.service';
import {TopicCard} from '../topic-card/topic-card';

/** How many distinct tags the "popular searches" shortcut row offers. */
const POPULAR_TAG_COUNT = 10;

/**
 * The full topic library: everything, with free-text search plus multi-select category and tag
 * filtering.
 *
 * These affordances live here rather than on the `/topics` landing page, which is a curated
 * narrative of four distinct sections with nowhere sensible for search results to land.
 *
 * **Filter semantics.** Categories and tags are one combined pool of interests joined by OR: any
 * topic matching *any* selected category or *any* selected tag is shown, so adding a selection
 * widens the result rather than narrowing it. Picking "Διατροφή", "Άσκηση" and the "Ενέργεια" tag
 * returns everything relating to any of the three. The free-text box is a different kind of
 * control — typed, not selected — so it narrows the result with AND.
 */
@Component({
    selector: 'app-all-health-topics',
    standalone: true,
    imports: [
        MatIconModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        TranslatePipe,
        TopicCard,
        ActiveFiltersToolbar,
    ],
    templateUrl: './all-health-topics.html',
    styleUrl: './all-health-topics.scss',
})
export class AllHealthTopics implements OnInit {

    private readonly destroyRef = inject(DestroyRef);
    private readonly dialog = inject(MatDialog);
    private readonly route = inject(ActivatedRoute);
    private readonly translate = inject(TranslateService);
    private readonly topicsService = inject(HealthTopicsService);
    private readonly favourites = inject(HealthTopicFavouritesService);
    private readonly tracking = inject(UsageTrackingService);

    private readonly topics = signal<HealthTopic[]>([]);

    readonly loading = signal(true);
    readonly loadError = signal(false);

    readonly categories: readonly HealthTopicCategory[] = HEALTH_TOPIC_CATEGORIES;

    // Seeded from the query string so a category or tag elsewhere in the app can deep-link here.
    // Both accept a comma-separated list.
    readonly selectedCategories = signal<ReadonlySet<HealthTopicCategory>>(this.readCategoryParam());
    readonly selectedTags = signal<ReadonlySet<string>>(this.readTagParam());
    readonly searchTerm = signal(this.route.snapshot.queryParamMap.get('q') ?? '');

    private readonly topicViews = computed<HealthTopicView[]>(() => {
        const favouriteIds = this.favourites.favouriteIds();
        return this.topics()
            .map(topic => ({
                ...topic,
                score: 0,
                matchedUserLabels: [],
                isFavourite: favouriteIds.has(topic.id),
            }))
            .sort((a, b) => a.displayOrder - b.displayOrder);
    });

    /**
     * OR across every selected category and tag, then AND with the typed term.
     *
     * Text search covers title, short text and tags only. The category is a code rendered through
     * the lexicon rather than free text, so matching the term against it would match nothing.
     */
    readonly filteredTopics = computed(() => {
        const categories = this.selectedCategories();
        const tags = this.selectedTags();
        const term = this.searchTerm().trim().toLowerCase();
        const hasSelection = categories.size > 0 || tags.size > 0;

        return this.topicViews().filter(topic => {
            const matchesSelection = !hasSelection
                || categories.has(topic.category)
                || topic.tags.some(tag => tags.has(tag));

            if (!matchesSelection) {
                return false;
            }
            if (!term) {
                return true;
            }

            return topic.title.toLowerCase().includes(term)
                || topic.brief.toLowerCase().includes(term)
                || topic.tags.some(tag => tag.toLowerCase().includes(term));
        });
    });

    readonly topicCount = computed(() => this.filteredTopics().length);

    readonly popularTags = computed(() => {
        const all = [...new Set(this.topicViews().flatMap(topic => topic.tags))]
            .slice(0, POPULAR_TAG_COUNT);

        // A tag selected via a deep link might fall outside the popular slice; show it anyway,
        // otherwise the pill row would contradict the filter chips.
        const selectedOutside = [...this.selectedTags()].filter(tag => !all.includes(tag));
        return [...all, ...selectedOutside];
    });

    /**
     * The current filters as chips, so they can be seen and dropped rather than only cleared
     * wholesale. Reuses the same toolbar the admin list pages use.
     *
     * No separate "are there filters?" flag is needed: the toolbar hides itself when this is empty.
     */
    readonly activeFilters = computed<ActiveFilter[]>(() => {
        const filters: ActiveFilter[] = [];

        const categories = [...this.selectedCategories()];
        if (categories.length) {
            filters.push({key: 'category', label: 'topics.all.filter.category', values: categories});
        }

        const tags = [...this.selectedTags()];
        if (tags.length) {
            filters.push({key: 'tags', label: 'topics.all.filter.tags', values: tags});
        }

        const term = this.searchTerm().trim();
        if (term) {
            filters.push({key: 'search', label: 'topics.all.filter.search', values: [term]});
        }

        return filters;
    });

    /**
     * An arrow property, not a method: the toolbar takes this as a plain input and calls it
     * unbound, so a prototype method would lose `this`.
     */
    readonly filterValueLabel = (key: string, value: string): string =>
        key === 'category' ? this.translate.instant('topics.category.' + value) as string : value;

    ngOnInit(): void {
        this.loadTopics();
        this.favourites.load();
    }

    searchTopics(event: Event): void {
        this.searchTerm.set((event.target as HTMLInputElement).value);
    }

    toggleCategory(category: HealthTopicCategory): void {
        this.selectedCategories.update(current => toggled(current, category));
    }

    isCategoryActive(category: HealthTopicCategory): boolean {
        return this.selectedCategories().has(category);
    }

    toggleTag(tag: string): void {
        this.selectedTags.update(current => toggled(current, tag));
    }

    isTagActive(tag: string): boolean {
        return this.selectedTags().has(tag);
    }

    /** "All" is a reset, not a value: it drops the category selection instead of joining it. */
    clearCategories(): void {
        this.selectedCategories.set(new Set());
    }

    get noCategorySelected(): boolean {
        return this.selectedCategories().size === 0;
    }

    /** Removes one whole chip from the filter banner. */
    removeFilter(key: string): void {
        if (key === 'category') {
            this.selectedCategories.set(new Set());
        } else if (key === 'tags') {
            this.selectedTags.set(new Set());
        } else if (key === 'search') {
            this.searchTerm.set('');
        }
    }

    clearFilters(): void {
        this.searchTerm.set('');
        this.selectedCategories.set(new Set());
        this.selectedTags.set(new Set());
    }

    openTopic(topic: HealthTopic): void {
        openHealthTopicDialog(this.dialog, topic);
    }

    toggleFavourite(topic: HealthTopicView): void {
        this.favourites.toggle(topic.id);
        this.tracking.trackInteraction(topic.matchedLabels);
    }

    /** Ignores unrecognised values rather than filtering everything away. */
    private readCategoryParam(): ReadonlySet<HealthTopicCategory> {
        const valid = new Set<string>(HEALTH_TOPIC_CATEGORIES);
        return new Set(
            this.readListParam('category').filter((v): v is HealthTopicCategory => valid.has(v)));
    }

    private readTagParam(): ReadonlySet<string> {
        return new Set(this.readTagParamValues());
    }

    private readTagParamValues(): string[] {
        return this.readListParam('tag');
    }

    private readListParam(name: string): string[] {
        return (this.route.snapshot.queryParamMap.get(name) ?? '')
            .split(',')
            .map(value => value.trim())
            .filter(Boolean);
    }

    private loadTopics(): void {
        this.topicsService.getTopics().pipe(
            takeUntilDestroyed(this.destroyRef),
            finalize(() => this.loading.set(false)),
        ).subscribe({
            next: topics => this.topics.set(topics),
            error: error => {
                console.error('Failed to load health topics', error);
                this.loadError.set(true);
            },
        });
    }
}

/** Adds or removes a value, always returning a new Set so the signal actually notifies. */
function toggled<T>(current: ReadonlySet<T>, value: T): ReadonlySet<T> {
    const next = new Set(current);
    if (next.has(value)) {
        next.delete(value);
    } else {
        next.add(value);
    }
    return next;
}

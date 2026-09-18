import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {MatDialog} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe} from '@ngx-translate/core';
import {finalize} from 'rxjs';
import {MatchReasons} from '../../shared/components/match-reasons/match-reasons';
import {SuggestionHero} from '../../shared/components/suggestion-hero/suggestion-hero';
import {UserProfileLabel} from '../../shared/interfaces/user-profile-label';
import {HealthProfileService} from '../../shared/services/health-profile.service';
import {labelPriorityMap, scoreAgainstLabels} from '../../shared/utils/label-match.util';
import {openHealthTopicDialog} from './health-topic-dialog/health-topic.dialog';
import {HealthTopicFavouritesService} from './health-topic-favourites.service';
import {UsageTrackingService} from '../../core/services/usage-tracking.service';
import {HealthTopic, HealthTopicView} from './health-topics.model';
import {HealthTopicsService} from './health-topics.service';
import {TopicCard} from './topic-card/topic-card';
import {TopicSection} from './topic-section/topic-section';

/** How many topics each section shows before the user has to go to the full library. */
const SUGGESTION_COUNT = 3;
const TRENDING_COUNT = 3;
const MORE_COUNT = 6;

/**
 * The topics landing page: a curated narrative rather than a catalogue.
 *
 * Suggested (from the user's own profiling labels) → their favourites → what is trending → a
 * taste of the rest, with "View all" leading to the searchable library at `/topics/all`. Search
 * and category filtering deliberately live on that page, not this one.
 */
@Component({
    selector: 'app-health-topics',
    standalone: true,
    imports: [MatIconModule, TranslatePipe, TopicCard, TopicSection, SuggestionHero, MatchReasons],
    templateUrl: './health-topics.html',
    styleUrl: './health-topics.scss',
})
export class HealthTopics implements OnInit {

    private readonly destroyRef = inject(DestroyRef);
    private readonly dialog = inject(MatDialog);
    private readonly topicsService = inject(HealthTopicsService);
    private readonly favourites = inject(HealthTopicFavouritesService);
    private readonly tracking = inject(UsageTrackingService);
    private readonly healthProfileService = inject(HealthProfileService);

    private readonly topics = signal<HealthTopic[]>([]);
    private readonly userLabels = signal<UserProfileLabel[]>([]);

    readonly loading = signal(true);
    readonly loadError = signal(false);
    readonly labelsError = signal(false);


    private readonly priorityMap = computed(() => labelPriorityMap(this.userLabels()));

    /** Every topic decorated with this user's favourite state. */
    private readonly topicViews = computed<HealthTopicView[]>(() => {
        const favouriteIds = this.favourites.favouriteIds();
        return this.topics().map(topic => ({
            ...topic,
            score: 0,
            matchedUserLabels: [],
            isFavourite: favouriteIds.has(topic.id),
        }));
    });

    /** Topics that actually match something in the user's profile, best first. */
    private readonly personalisedSuggestions = computed<HealthTopicView[]>(() => {
        const priorities = this.priorityMap();
        if (priorities.size === 0) {
            return [];
        }

        return this.topicViews()
            .map(topic => {
                const match = scoreAgainstLabels(topic, priorities);
                return {...topic, score: match.score, matchedUserLabels: match.matchedLabels};
            })
            .filter(topic => topic.score > 0)
            .sort((a, b) => b.score - a.score)
            .slice(0, SUGGESTION_COUNT);
    });

    /**
     * Shown when personalisation has nothing to work with: a brand-new account, a profile that was
     * never completed, optional labels cleared by a profile re-save, or a failed labels request.
     * The card must never be blank, so this has no conditions of its own.
     */
    private readonly fallbackSuggestions = computed<HealthTopicView[]>(() =>
        [...this.topicViews()]
            .sort((a, b) =>
                (a.trendingRank ?? Number.MAX_SAFE_INTEGER) - (b.trendingRank ?? Number.MAX_SAFE_INTEGER)
                || a.displayOrder - b.displayOrder)
            .slice(0, SUGGESTION_COUNT));

    readonly isPersonalised = computed(() => this.personalisedSuggestions().length > 0);

    readonly heroTopics = computed(() =>
        this.isPersonalised() ? this.personalisedSuggestions() : this.fallbackSuggestions());

    /** How many distinct profile signals drove the suggestions, for the match summary line. */
    readonly matchedSignalCount = computed(() =>
        new Set(this.heroTopics().flatMap(topic => topic.matchedUserLabels)).size);

    readonly favouriteTopics = computed(() => this.topicViews().filter(topic => topic.isFavourite));

    readonly trendingTopics = computed(() =>
        this.topicViews()
            .filter(topic => topic.trendingRank !== undefined)
            .sort((a, b) => (a.trendingRank ?? 0) - (b.trendingRank ?? 0))
            .slice(0, TRENDING_COUNT));

    /** Whatever the user has not already seen further up the page. */
    readonly moreTopics = computed(() => {
        const shown = new Set(
            [...this.heroTopics(), ...this.favouriteTopics(), ...this.trendingTopics()]
                .map(topic => topic.id));

        return this.topicViews()
            .filter(topic => !shown.has(topic.id))
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .slice(0, MORE_COUNT);
    });

    ngOnInit(): void {
        this.loadTopics();
        this.loadLabels();
        this.favourites.load();
    }


    openTopic(topic: HealthTopic): void {
        openHealthTopicDialog(this.dialog, topic);
    }

    toggleFavourite(topic: HealthTopicView): void {
        this.favourites.toggle(topic.id);
        this.tracking.trackInteraction(topic.matchedLabels);
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

    private loadLabels(): void {
        this.healthProfileService.getMyLabels().pipe(
            takeUntilDestroyed(this.destroyRef),
        ).subscribe({
            next: labels => this.userLabels.set(labels),
            error: error => {
                // Not fatal: the hero falls back to popular topics rather than showing nothing.
                console.error('Failed to load profile labels', error);
                this.labelsError.set(true);
            },
        });
    }
}

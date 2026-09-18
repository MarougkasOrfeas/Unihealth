import {Component, computed, DestroyRef, ElementRef, inject, OnInit, signal, ViewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {RouterLink} from "@angular/router";
import {RssFeedService} from "../../shared/services/rss-feed.service";
import {AsyncPipe, DatePipe, NgForOf} from "@angular/common";
import {map, Observable} from "rxjs";
import {MatDialog} from "@angular/material/dialog";
import {MatIcon} from "@angular/material/icon";
import {RssFeed} from "../../shared/interfaces/rss-feed";
import {TranslatePipe} from "@ngx-translate/core";
import {openHealthTopicDialog} from "../health-topics/health-topic-dialog/health-topic.dialog";
import {HealthTopicFavouritesService} from "../health-topics/health-topic-favourites.service";
import {HealthTopic, HealthTopicView} from "../health-topics/health-topics.model";
import {HealthTopicsService} from "../health-topics/health-topics.service";
import {TopicCard} from "../health-topics/topic-card/topic-card";
import {TopicSection} from "../health-topics/topic-section/topic-section";
import {UsageTrackingService} from '../../core/services/usage-tracking.service';

interface HomeRssFeed extends RssFeed {
    shortSummary: string;
}

/** How many topic cards the home page shows before sending the user to /topics. */
const HOME_TOPIC_COUNT = 3;

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [
        RouterLink,
        DatePipe,
        NgForOf,
        AsyncPipe,
        MatIcon,
        TranslatePipe,
        TopicCard,
        TopicSection
    ],
    templateUrl: './home.html',
    styleUrl: './home.scss',
})
export class Home implements OnInit {
    isAtStart = true;
    isAtEnd = false;
    rssFeeds$!: Observable<HomeRssFeed[]>;

    @ViewChild('carousel', {static: false}) carousel!: ElementRef;

    private readonly destroyRef = inject(DestroyRef);
    private readonly dialog = inject(MatDialog);
    private readonly topicsService = inject(HealthTopicsService);
    private readonly favourites = inject(HealthTopicFavouritesService);
    private readonly tracking = inject(UsageTrackingService);

    private readonly topics = signal<HealthTopic[]>([]);

    private readonly topicViews = computed<HealthTopicView[]>(() => {
        const favouriteIds = this.favourites.favouriteIds();
        return this.topics().map(topic => ({
            ...topic,
            score: 0,
            matchedUserLabels: [],
            isFavourite: favouriteIds.has(topic.id),
        }));
    });

    private readonly favouriteTopics = computed(() =>
        this.topicViews()
            .filter(topic => topic.isFavourite)
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .slice(0, HOME_TOPIC_COUNT));

    private readonly trendingTopics = computed(() =>
        this.topicViews()
            .filter(topic => topic.trendingRank !== undefined)
            .sort((a, b) => (a.trendingRank ?? 0) - (b.trendingRank ?? 0))
            .slice(0, HOME_TOPIC_COUNT));

    /** The user's own saved topics take precedence; trending is the fallback for everyone else. */
    readonly showingFavourites = computed(() => this.favouriteTopics().length > 0);

    readonly homeTopics = computed(() =>
        this.showingFavourites() ? this.favouriteTopics() : this.trendingTopics());

    constructor(private homeLogicService: RssFeedService) {
    }

    ngOnInit(): void {
        this.rssFeeds$ = this.homeLogicService.findRelevantFeeds().pipe(
            map(feeds => feeds.map(feed => ({
                ...feed,
                shortSummary: this.truncateText(this.htmlToPlainText(feed.summary || ''), 130)
            })))
        );

        this.rssFeeds$.subscribe(() => {
            setTimeout(() => this.checkScrollPosition(), 100);
        });

        this.loadTopics();
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
        ).subscribe({
            // The section simply stays hidden on failure: topics are secondary on this page.
            next: topics => this.topics.set(topics),
            error: error => console.error('Failed to load health topics', error),
        });
    }

    ngAfterViewInit(): void {
        setTimeout(() => this.checkScrollPosition(), 0);
    }

    onImageError(event: Event): void {
        const img = event.target as HTMLImageElement;
        img.src = 'assets/placeholder.jpg';
    }

    openLink(url: string) {
        window.open(url, '_blank');
    }

    nextSlide() {
        const container = this.carousel.nativeElement;
        const cardWidth = 320;
        container.scrollBy({left: cardWidth, behavior: 'smooth'});
        setTimeout(() => this.checkScrollPosition(), 350);
    }

    prevSlide() {
        const container = this.carousel.nativeElement;
        const cardWidth = 320;
        container.scrollBy({left: -cardWidth, behavior: 'smooth'});
        setTimeout(() => this.checkScrollPosition(), 350);
    }

    checkScrollPosition() {
        if (!this.carousel) return;

        const container = this.carousel.nativeElement;
        this.isAtStart = container.scrollLeft <= 5;
        this.isAtEnd = container.scrollLeft + container.clientWidth >= container.scrollWidth - 5;
    }

    private htmlToPlainText(html: string): string {
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        return (tempDiv.textContent || tempDiv.innerText || '').replace(/\s+/g, ' ').trim();
    }

    private truncateText(text: string, maxLength: number): string {
        if (!text || text.length <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim() + '...';
    }

}

import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {RouterLink} from "@angular/router";
import {RssFeedService} from "../../shared/services/rss-feed.service";
import {AsyncPipe, DatePipe, NgForOf} from "@angular/common";
import {map, Observable} from "rxjs";
import {MatIcon} from "@angular/material/icon";
import {RssFeed} from "../../shared/interfaces/rss-feed";
import {TranslatePipe} from "@ngx-translate/core";

interface HomeRssFeed extends RssFeed {
    shortSummary: string;
}

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [
        RouterLink,
        DatePipe,
        NgForOf,
        AsyncPipe,
        MatIcon,
        TranslatePipe
    ],
    templateUrl: './home.html',
    styleUrl: './home.scss',
})
export class Home implements OnInit {
    isAtStart = true;
    isAtEnd = false;
    rssFeeds$!: Observable<HomeRssFeed[]>;

    @ViewChild('carousel', {static: false}) carousel!: ElementRef;

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

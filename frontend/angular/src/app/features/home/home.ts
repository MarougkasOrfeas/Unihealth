import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {RouterLink} from "@angular/router";
import {HomeService} from "../../shared/services/home.service";
import {AsyncPipe, DatePipe, NgForOf, NgIf} from "@angular/common";
import {RssFeed} from "../../shared/interfaces/rss-feed";
import {Observable} from "rxjs";
import {MatIcon} from "@angular/material/icon";

@Component({
    selector: 'app-home',
    imports: [
        RouterLink,
        DatePipe,
        NgForOf,
        AsyncPipe,
        NgIf,
        MatIcon
    ],
    templateUrl: './home.html',
    styleUrl: './home.scss',
})
export class Home implements OnInit {
    isAtStart: boolean = true;
    isAtEnd: boolean = false;
    rssFeeds$!: Observable<RssFeed[]>;

    @ViewChild('carousel', {static: false}) carousel!: ElementRef;

    constructor(private homeLogicService: HomeService) {
    }

    ngOnInit(): void {
        this.rssFeeds$ = this.homeLogicService.getLatestFeeds(10);
    }

    openLink(url: string) {
        window.open(url, '_blank');
    }

    nextSlide() {
        const container = this.carousel.nativeElement;
        const cardWidth = 400; // Adjust based on actual card size including gap
        container.scrollBy({left: cardWidth, behavior: 'smooth'});
        setTimeout(() => this.checkScrollPosition(), 300);
    }

    prevSlide() {
        const container = this.carousel.nativeElement;
        const cardWidth = 400;
        container.scrollBy({left: -cardWidth, behavior: 'smooth'});
        setTimeout(() => this.checkScrollPosition(), 300);
    }

    checkScrollPosition() {
        const container = this.carousel.nativeElement;
        this.isAtStart = container.scrollLeft <= 0;
        this.isAtEnd = container.scrollLeft + container.clientWidth >= container.scrollWidth;
    }

}

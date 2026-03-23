import {Component, inject, OnInit} from "@angular/core";
import {RssFeed} from "../../shared/interfaces/rss-feed";
import {RssFeedService} from "../../shared/services/rss-feed.service";
import {DatePipe} from "@angular/common";
import {TranslatePipe} from "@ngx-translate/core";


@Component({

    selector: "app-health-news",
    imports: [
        DatePipe,
        TranslatePipe
    ],
    templateUrl: "./health-news.html",
    styleUrl: './health-news.scss'
})
export class HealthNews implements OnInit {

    feeds: RssFeed[] = [];
    pageNumber = 0;
    pageSize = 15;
    totalElements = 0;
    totalPages = 0;
    isLoading = false;


    private service = inject(RssFeedService);

    ngOnInit(): void {
        this.loadFeeds();
    }

    loadFeeds(page: number = 0): void {
        this.isLoading = true;

        this.service.getPage({
            page,
            size: this.pageSize,
            sort: ['publishedDate,desc']
        }).subscribe({
            next: (pageResponse) => {
                this.feeds = pageResponse.content ?? [];
                this.totalElements = pageResponse.totalElements ?? 0;
                this.pageNumber = pageResponse.number ?? 0;
                this.pageSize = pageResponse.size ?? 12;
                this.totalPages = this.pageSize > 0
                    ? Math.ceil(this.totalElements / this.pageSize)
                    : 0;
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Failed to load health news', err);
                this.feeds = [];
                this.totalElements = 0;
                this.totalPages = 0;
                this.isLoading = false;
            }
        });
    }

    nextPage(): void {
        if (this.pageNumber + 1 < this.totalPages) {
            this.loadFeeds(this.pageNumber + 1);
        }
    }

    prevPage(): void {
        if (this.pageNumber > 0) {
            this.loadFeeds(this.pageNumber - 1);
        }
    }

    trackByFeed(index: number, item: RssFeed): string | number {
        return item.id ?? item.link ?? index;
    }
}

import {Injectable} from "@angular/core";
import {BaseService} from "./base.service";
import {RssFeed} from "../interfaces/rss-feed";
import {map, Observable} from "rxjs";


@Injectable({providedIn: 'root'})
export class HomeService extends BaseService<RssFeed> {

    constructor() {
        super("home");
    }

    getLatestFeeds(limit = 10): Observable<RssFeed[]> {
        return this.httpClient.post<RssFeed[]>(`${this.basePath}/rss`, {limit}).pipe(
            map(items => {
                items.forEach(item => this.transform(item));
                return items;
            })
        );
    }

}

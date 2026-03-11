import {Injectable} from "@angular/core";
import {BaseService} from "./base.service";
import {RssFeed} from "../interfaces/rss-feed";
import {map, Observable} from "rxjs";
import {UNIHEALTH_CONSTANTS} from "../constants/unihealth.constants";

@Injectable({providedIn: 'root'})
export class RssFeedService extends BaseService<RssFeed> {

    constructor() {
        super("rss");
    }

    findRelevantFeeds(): Observable<RssFeed[]> {
        return this.httpClient.post<RssFeed[]>(
            BaseService.CONTEXT_PATH + UNIHEALTH_CONSTANTS.RSS_API.HOME,
            {}
        ).pipe(
            map(items => {
                items.forEach(item => this.transform(item));
                return items;
            })
        );
    }

}

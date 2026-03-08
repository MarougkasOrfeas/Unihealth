import {BaseUpdateableEntity} from "./baseEntity";

export interface RssFeed extends BaseUpdateableEntity {
    title: string;
    summary: string;
    link: string;
    imageUrl?: string | null;
    publishedDate: Date | string;
    sourceName?: string;
}

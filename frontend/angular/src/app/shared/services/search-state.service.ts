import {Injectable, signal} from '@angular/core';
import {MultiSortEntry} from '../components/base-table/base-table';

/**
 * A list screen's query, captured so navigating to a detail page and back returns to the same
 * filters, sort and page. Plain data on purpose, so it could later move into the URL as query
 * params without changing shape.
 */
export interface ListQuerySnapshot {
    readonly search: string;
    /** Column key to the raw backend values selected for it. */
    readonly facets: Readonly<Record<string, readonly string[]>>;
    readonly sort: readonly MultiSortEntry[];
    readonly pageIndex: number;
    readonly pageSize: number;
}

/**
 * Holds one snapshot per list. Keyed by list name so several lists can each remember their own
 * query, rather than the most recent one overwriting the rest.
 */
@Injectable({providedIn: 'root'})
export class SearchStateService {

    private readonly snapshots = signal<ReadonlyMap<string, ListQuerySnapshot>>(new Map());

    save(listName: string, snapshot: ListQuerySnapshot): void {
        this.snapshots.update((all) => new Map(all).set(listName, snapshot));
    }

    /**
     * Returns the snapshot and drops it, so it is consumed by exactly one restore — re-entering
     * the list from the navigation menu starts clean instead of resurrecting an old query.
     */
    take(listName: string): ListQuerySnapshot | null {
        const snapshot = this.snapshots().get(listName) ?? null;
        if (snapshot) {
            this.snapshots.update((all) => {
                const next = new Map(all);
                next.delete(listName);
                return next;
            });
        }
        return snapshot;
    }

    clear(): void {
        this.snapshots.set(new Map());
    }
}

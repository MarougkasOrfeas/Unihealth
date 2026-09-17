import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterModule} from '@angular/router';
import {filter} from 'rxjs/operators';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {TranslateModule, TranslatePipe} from "@ngx-translate/core";
import {MatTooltip} from "@angular/material/tooltip";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

type Crumb = { label: string; url: string };

/**
 * Breadcrumbs component that builds a breadcrumb trail based on the current route.
 * It listens to route changes and updates the breadcrumbs accordingly.
 * Each breadcrumb is defined by a 'breadcrumb' property in the route's data.
 */
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-breadcrumbs',
    standalone: true,
    imports: [RouterModule, MatButtonModule, MatIconModule, TranslateModule, MatTooltip],
    templateUrl: './breadcrumbs.html',
    styleUrls: ['./breadcrumbs.scss'],
})
export class BreadcrumbsComponent {

    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly destroyRef = inject(DestroyRef);

    private readonly _crumbs = signal<Crumb[]>([]);
    readonly crumbs = computed(() => this._crumbs());

    /**
     * Initialize the component and set up a listener for route changes to update the breadcrumbs.
     */
    ngOnInit(): void {
        this.router.events
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((e) => e instanceof NavigationEnd),
            )
            .subscribe(() => {
                this._crumbs.set(this.buildCrumbs(this.route.root));
            });

        this._crumbs.set(this.buildCrumbs(this.route.root));
    }

    /**
     * Recursively builds the breadcrumb trail by traversing the route tree.
     * It constructs the URL for each breadcrumb and collects the label from the route's data.
     * @param route The current route to process.
     * @param url The accumulated URL up to the current route.
     * @param acc The accumulated list of breadcrumbs collected so far.
     * @returns An array of Crumb objects representing the breadcrumb trail.
     * @private
     */
    private buildCrumbs(route: ActivatedRoute, url = '', acc: Crumb[] = []): Crumb[] {
        const child = route.firstChild;
        if (!child) {
            return acc;
        }

        const part = child.snapshot.url.map((s) => s.path).join('/');
        const nextUrl = part ? `${url}/${part}` : url;

        const label = child.snapshot.data['breadcrumb'] as string | undefined;
        if (label) {
            acc.push({label, url: nextUrl || '/'});
        }

        return this.buildCrumbs(child, nextUrl, acc);
    }
}

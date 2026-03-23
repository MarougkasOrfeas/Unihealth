import {Component, computed, signal} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterModule} from '@angular/router';
import {filter} from 'rxjs/operators';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe} from "@ngx-translate/core";

type Crumb = { label: string; url: string };

@Component({
    selector: 'app-breadcrumbs',
    standalone: true,
    imports: [RouterModule, MatButtonModule, MatIconModule, TranslatePipe],
    templateUrl: './breadcrumbs.html',
    styleUrls: ['./breadcrumbs.scss'],
})
export class BreadcrumbsComponent {
    private readonly _crumbs = signal<Crumb[]>([]);
    readonly crumbs = computed(() => this._crumbs());

    constructor(private router: Router, private route: ActivatedRoute) {
        this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
            this._crumbs.set(this.buildCrumbs(this.route.root));
        });

        this._crumbs.set(this.buildCrumbs(this.route.root));
    }

    private buildCrumbs(route: ActivatedRoute, url = '', acc: Crumb[] = []): Crumb[] {
        const child = route.firstChild;
        if (!child) return acc;

        const part = child.snapshot.url.map((s) => s.path).join('/');
        const nextUrl = part ? `${url}/${part}` : url;

        const label = child.snapshot.data['breadcrumb'] as string | undefined;
        if (label) acc.push({label, url: nextUrl || '/'});

        return this.buildCrumbs(child, nextUrl, acc);
    }
}

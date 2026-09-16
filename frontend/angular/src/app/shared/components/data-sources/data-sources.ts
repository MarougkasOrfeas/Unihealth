import {Component, inject, signal} from "@angular/core";
import {CommonModule} from "@angular/common";
import {MatIconModule} from "@angular/material/icon";
import {DataSourceService} from "../../services/data-source.service";
import {DataSource} from "../../interfaces/data-source";

/**
 * Credits the corpora the health content was ingested from.
 *
 * <p>Not decoration: the NHS content is reused under the Open Government Licence, which requires
 * attribution, so this is shown on every page that renders it.
 */
@Component({
    selector: 'app-data-sources',
    standalone: true,
    imports: [
        CommonModule,
        MatIconModule
    ],
    templateUrl: './data-sources.html',
    styleUrl: './data-sources.scss'
})
export class DataSources {

    private dataSourceService = inject(DataSourceService);

    sources = signal<DataSource[]>([]);

    constructor() {
        this.dataSourceService.findContent().subscribe({
            next: data => this.sources.set(data),
            error: err => {
                console.error('Error loading data sources', err);
                this.sources.set([]);
            }
        });
    }
}

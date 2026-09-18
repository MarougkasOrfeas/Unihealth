import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';

/**
 * What the application stores on the user's device and what it measures, behind the footer's
 * "Cookies Policy" link.
 *
 * Entirely lexicon-driven: the text is UI chrome, not ingested content, so it lives in the QLACK
 * lexicon alongside everything else rather than in a database table or a hardcoded string.
 */
@Component({
    selector: 'app-cookies-policy',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatIconModule, MatButtonModule, RouterLink, TranslatePipe],
    templateUrl: './cookies-policy.html',
    styleUrl: './cookies-policy.scss',
})
export class CookiesPolicy {
}

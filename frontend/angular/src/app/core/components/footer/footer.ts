import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';

@Component({
    selector: 'app-footer',
    standalone: true,
    imports: [
        TranslateModule
    ],
    templateUrl: './footer.html',
    styleUrls: ['./footer.scss']
})
export class Footer {

}

import {Component, inject, signal} from '@angular/core';
import {Footer} from "./core/components/footer/footer";
import {Header} from "./core/components/header/header";
import {SideNav} from "./core/components/side-nav/side-nav";
import {TranslateService} from "@ngx-translate/core";
import {forkJoin} from "rxjs";
import {AuthService} from "./core/auth/auth.service";

@Component({
    selector: 'app-root',
    imports: [
        Footer,
        Header,
        SideNav,
    ],
    templateUrl: './app.html',
    styleUrl: './app.scss'
})
export class App {
    private translate = inject(TranslateService);
    private authService = inject(AuthService);

    protected loggedIn = signal(false);

    constructor() {
        forkJoin([
            this.translate.setFallbackLang('el'),
            this.translate.use('el')
        ]).subscribe(() => {
            this.loggedIn.set(this.authService.authenticated);
        });
    }

}

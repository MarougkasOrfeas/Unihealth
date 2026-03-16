import {Component, inject, signal} from '@angular/core';
import {Footer} from "./core/components/footer/footer";
import {Header} from "./core/components/header/header";
import {SideNav} from "./core/components/side-nav/side-nav";
import {TranslateService} from "@ngx-translate/core";
import {forkJoin} from "rxjs";
import {AuthService} from "./core/auth/auth.service";
import {MatIcon} from "@angular/material/icon";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {UniHealthAssistantComponent} from "./core/components/unihealth-assistant/unihealth-assistant";
import {HealthProfileFormComponent} from "./core/components/health-profile-form/health-profile-form";

@Component({
    selector: 'app-root',
    imports: [
        Footer,
        Header,
        SideNav,
        MatIcon,
        HealthProfileFormComponent,
    ],
    templateUrl: './app.html',
    styleUrl: './app.scss'
})
export class App {
    private translate = inject(TranslateService);
    private authService = inject(AuthService);
    private dialog = inject(MatDialog);

    protected loggedIn = signal(false);
    protected assistantOpen = signal(false);

    private assistantDialogRef: MatDialogRef<UniHealthAssistantComponent> | null = null;

    isFirstTime = this.authService.isFirstTime;

    constructor() {
        forkJoin([
            this.translate.setFallbackLang('el'),
            this.translate.use('el')
        ]).subscribe(() => {
            this.loggedIn.set(this.authService.authenticated);
        });
    }

    openHealthAssistant(): void {
        if (this.assistantDialogRef) {
            this.assistantDialogRef.close();
            return;
        }

        this.assistantOpen.set(true);

        this.assistantDialogRef = this.dialog.open(UniHealthAssistantComponent, {
            width: '430px',
            maxWidth: '95vw',
            panelClass: 'health-assistant-dialog',
            autoFocus: false,
            disableClose: true,
            hasBackdrop: true,
            position: {
                right: '70px',
                bottom: '60px'
            }
        });

        this.assistantDialogRef.afterClosed().subscribe(() => {
            this.assistantDialogRef = null;
            this.assistantOpen.set(false);
        });
    }

}

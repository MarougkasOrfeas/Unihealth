import {Component, effect, inject, signal} from '@angular/core';
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
import {AiAssistantBubble} from "./core/components/ai-assistant-bubble/ai-assistant-bubble";
import {AnalyticsConsentDialog} from "./core/components/analytics-consent-dialog/analytics-consent-dialog";
import {AnalyticsConsentService} from "./core/services/analytics-consent.service";

@Component({
    selector: 'app-root',
    imports: [
        Footer,
        Header,
        SideNav,
        MatIcon,
        HealthProfileFormComponent,
        AiAssistantBubble,
    ],
    templateUrl: './app.html',
    styleUrl: './app.scss'
})
export class App {
    private translate = inject(TranslateService);
    private authService = inject(AuthService);
    private dialog = inject(MatDialog);
    private consent = inject(AnalyticsConsentService);

    protected loggedIn = signal(false);
    protected assistantOpen = signal(false);

    private assistantDialogRef: MatDialogRef<UniHealthAssistantComponent> | null = null;
    private consentAsked = false;

    isFirstTime = this.authService.isFirstTime;

    constructor() {
        forkJoin([
            this.translate.setFallbackLang('el'),
            this.translate.use('el')
        ]).subscribe(() => {
            this.loggedIn.set(this.authService.authenticated);

            if (this.authService.authenticated) {
                this.consent.load();
            }
        });

        // Waits for the answer to actually be known. `mustAsk` stays false while loading and while
        // the request is failing, so the dialog never flashes and a broken backend never prompts.
        effect(() => {
            if (this.loggedIn() && !this.isFirstTime() && this.consent.mustAsk()) {
                this.askForConsent();
            }
        });
    }

    /**
     * Shown once per account, never behind the first-run health profile form — being asked two
     * blocking questions back to back is worse than asking on the next visit.
     */
    private askForConsent(): void {
        if (this.consentAsked) {
            return;
        }
        this.consentAsked = true;

        this.dialog.open(AnalyticsConsentDialog, {
            width: '520px',
            maxWidth: '94vw',
            autoFocus: false,
            // Must be answered — but both answers are a single click.
            disableClose: true,
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

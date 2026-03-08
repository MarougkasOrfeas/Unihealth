import {Component, computed, inject, signal, ViewChild} from "@angular/core";
import {MatToolbar} from "@angular/material/toolbar";
import {MatButton, MatIconButton} from "@angular/material/button";
import {MatIcon} from "@angular/material/icon";
import {MatMenu, MatMenuItem, MatMenuTrigger} from "@angular/material/menu";
import {MatTooltip} from "@angular/material/tooltip";
import {RouterLink, RouterOutlet} from "@angular/router";
import {NgOptimizedImage} from "@angular/common";
import {BeforeUnloadService} from "../../../shared/services/beforeunload.service";
import {AuthService} from "../../auth/auth.service";
import {Observable, of} from "rxjs";
import {CanLeaveWithUnsavedChanges} from "../../../shared/interfaces/unsaved-changes";
import {TranslatePipe} from "@ngx-translate/core";


@Component({
    selector: 'app-header',
    imports: [
        MatToolbar,
        MatButton,
        MatIcon,
        MatMenu,
        MatMenuTrigger,
        MatMenuItem,
        MatIconButton,
        MatTooltip,
        RouterLink,
        NgOptimizedImage,
        TranslatePipe
    ],
    templateUrl: './header.html',
    styleUrls: ['./header.scss']
})
export class Header {
    /**
     * Service responsible for skipping the native browser "Leave site?".
     */
    private beforeUnload = inject(BeforeUnloadService);

    /**
     * Service responsible for initializing OAuth / authentication.
     */
    private oauthService = inject(AuthService);

    /**
     * Stores the username of the currently logged-in user.
     */
    protected username = signal<string | null>(this.oauthService.username ?? null);

    /**
     * Used for logout: if the current page has unsaved changes, it can show its own
     * confirmation dialog before logout is executed.
     */
    @ViewChild(RouterOutlet) outlet!: RouterOutlet;


    protected calculateIconLetter = computed(
        () => this.username()?.[0]?.toUpperCase() ?? 'A' // Default value
    );

    logout(): void {
        this.canLeaveCurrentPage().subscribe((canLeave) => {
            if (!canLeave) return;
            this.beforeUnload.skipOnce();
            this.oauthService.logout(false);
        });
    }

    /**
     * Checks if the currently active routed page allows leaving.
     *
     * Pages that implement {@link CanLeaveWithUnsavedChanges} can prevent leaving
     * (for example create user page with unsaved changes).
     */
    private canLeaveCurrentPage(): Observable<boolean> {
        const cmp = this.outlet?.component as Partial<CanLeaveWithUnsavedChanges> | null;
        // Pages without unsaved-changes logic allow leaving immediately.
        if (!cmp || typeof cmp.canDeactivate !== 'function') {
            return of(true);
        }
        const res = cmp.canDeactivate();
        return typeof res === 'boolean' ? of(res) : res;
    }

}

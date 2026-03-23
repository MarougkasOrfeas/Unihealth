import {Component, inject} from "@angular/core";
import {BreakpointObserver, Breakpoints} from "@angular/cdk/layout";
import {map, shareReplay} from "rxjs";
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from "@angular/material/sidenav";
import {AsyncPipe} from "@angular/common";
import {BreadcrumbsComponent} from "../../../shared/components/breadcrumb/breadcrumbs";
import {
    MatAccordion,
    MatExpansionPanel,
    MatExpansionPanelHeader,
    MatExpansionPanelTitle
} from "@angular/material/expansion";
import {MatIcon} from "@angular/material/icon";
import {MatIconButton} from "@angular/material/button";
import {MatListItem, MatListItemIcon, MatListItemTitle, MatNavList} from "@angular/material/list";
import {MatToolbar} from "@angular/material/toolbar";
import {RouterLink, RouterOutlet} from "@angular/router";
import {TranslatePipe} from "@ngx-translate/core";


@Component({
    selector: 'app-side-nav',
    imports: [
        AsyncPipe,
        BreadcrumbsComponent,
        MatAccordion,
        MatExpansionPanel,
        MatExpansionPanelHeader,
        MatExpansionPanelTitle,
        MatIcon,
        MatIconButton,
        MatListItem,
        MatListItemIcon,
        MatListItemTitle,
        MatNavList,
        MatSidenav,
        MatSidenavContainer,
        MatSidenavContent,
        MatToolbar,
        RouterLink,
        RouterOutlet,
        TranslatePipe
    ],
    templateUrl: './side-nav.html',
    styleUrl: './side-nav.scss'
})
export class SideNav {
    /**
     * Observes screen size changes to enable responsive layout behavior.
     */
    private breakpoint = inject(BreakpointObserver);

    /**
     * Emits `true` when the application is displayed on a handset-sized screen.
     */
    isHandset$ = this.breakpoint.observe(Breakpoints.Handset).pipe(
        map((r) => r.matches),
        shareReplay(1)
    );

    /**
     * Closes the sidenav only when running on a handset-sized screen.
     * This keeps the sidenav open on larger screens.
     *
     * @param sidenav Reference to the Material sidenav instance
     */
    closeIfHandset(sidenav: MatSidenav) {
        this.isHandset$.subscribe(isHandset => {
            if (isHandset) sidenav.close();
        }).unsubscribe();
    }
}



import {Directive, inject, OnInit, TemplateRef, ViewContainerRef} from "@angular/core";
import {UserService} from "../../shared/services/user.service";
import {catchError, of, tap} from "rxjs";
import {Permission} from "../interface/permission";


@Directive({
    selector: '[appHasAdminPermission]',
    standalone: true,
})
export class HasAdminPermissionDirective implements OnInit {

    private userService = inject(UserService);
    private templateRef = inject(TemplateRef<any>);
    private vcr = inject(ViewContainerRef);

    ngOnInit(): void {
        this.userService
            .getLoggedinUserRightsMatrix()
            .pipe(
                tap((rightsMatrix) => {
                    this.vcr.clear();
                    const perms: string[] = rightsMatrix?.globalPermissions ?? [];
                    const hasAccess = perms.includes(Permission.ADMIN);

                    if (hasAccess) {
                        this.vcr.createEmbeddedView(this.templateRef);
                    }
                }),
                catchError((err) => {
                    this.vcr.clear();
                    console.error('Error fetching permissions:', err);
                    return of(null);
                }),
            )
            .subscribe();
    }
}

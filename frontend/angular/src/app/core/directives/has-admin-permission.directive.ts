import {Directive, effect, inject, TemplateRef, ViewContainerRef} from "@angular/core";
import {PermissionService} from "../auth/permission.service";

/**
 * Renders its content only for administrators.
 *
 * Reads the cached `isAdmin` signal, so however many instances are on the page they share a single
 * rights-matrix request, and all of them flip together once it resolves.
 */
@Directive({
    selector: '[appHasAdminPermission]',
    standalone: true,
})
export class HasAdminPermissionDirective {

    constructor() {
        const permissions = inject(PermissionService);
        const templateRef = inject(TemplateRef<unknown>);
        const vcr = inject(ViewContainerRef);

        effect(() => {
            vcr.clear();
            if (permissions.isAdmin()) {
                vcr.createEmbeddedView(templateRef);
            }
        });
    }
}

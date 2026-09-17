import {CanDeactivateFn} from '@angular/router';
import {Observable} from 'rxjs';
import {CanLeaveWithUnsavedChanges} from '../../shared/interfaces/unsaved-changes';

/**
 * Lets a form block navigation while it has unsaved edits.
 *
 * Delegates entirely: the component decides whether to allow the exit and owns the confirmation
 * prompt, which is what `CanLeaveWithUnsavedChanges` already assumed. The contract existed and was
 * implemented, but no route ever referenced a guard — so nothing enforced it until now.
 */
export const unsavedChangesGuard: CanDeactivateFn<CanLeaveWithUnsavedChanges> = (
    component,
): boolean | Observable<boolean> => component.canDeactivate();

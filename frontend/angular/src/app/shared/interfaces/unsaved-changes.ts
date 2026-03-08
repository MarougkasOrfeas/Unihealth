import {Observable} from 'rxjs';

export interface CanLeaveWithUnsavedChanges {
  canDeactivate: () => boolean | Observable<boolean>;
}

import {Directive, HostListener, inject, Input} from '@angular/core';
import {BeforeUnloadService} from '../services/beforeunload.service';

@Directive({
  selector: '[appUnsavedChangesBeforeUnload]',
  standalone: true,
})
export class UnsavedChangesBeforeUnloadDirective {
  @Input() appUnsavedChangesBeforeUnload = false;

  /** Service used to skip the native browser "Leave site?" */
  private beforeUnload = inject(BeforeUnloadService);

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent) {
    // This is added to skip the browser leave page for logout option.
    if (this.beforeUnload.consumeSkipOnce()) return;

    if (!this.appUnsavedChangesBeforeUnload) return;
    event.preventDefault();
    event.returnValue = '';
  }
}

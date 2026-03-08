import {Injectable} from '@angular/core';

@Injectable({providedIn: 'root'})
export class BeforeUnloadService {
    /** Used to temporarily disable the browser "Leave site?" warning.*/
    private skip = false;

    /**
     * Marks the next "beforeunload" event to be ignored once.
     * Call this right before triggering a logout / redirect.
     */
    skipOnce() {
        this.skip = true;
    }

    /**
     * Consumes the skip flag.
     * Used inside the "beforeunload" listener.
     *
     * @returns true if the next beforeunload should be skipped, otherwise false.
     */
    consumeSkipOnce(): boolean {
        const v = this.skip;
        this.skip = false;
        return v;
    }
}

import {computed, inject, Injectable, signal} from '@angular/core';
import {UserPreferences} from '../../shared/interfaces/user';
import {UserService} from '../../shared/services/user.service';

/**
 * Whether the user has agreed to usage measurement, and the single place that answer is read from.
 *
 * Tri-state, matching `AuthService.healthProfileCompleted`: `null` means the answer is not known
 * yet — either still loading or never asked — and is what makes the consent dialog appear. It is
 * deliberately distinct from `false`, which means the user declined and should not be asked again.
 *
 * Every failure path leaves the value `null`, so a request error results in collecting nothing
 * rather than assuming permission.
 */
@Injectable({providedIn: 'root'})
export class AnalyticsConsentService {

    private readonly userService = inject(UserService);

    private readonly consent = signal<boolean | null>(null);

    /** Read by the tracker before it buffers anything, and by the shell to decide whether to ask. */
    readonly analyticsConsent = this.consent.asReadonly();

    readonly granted = computed(() => this.consent() === true);

    /** True only once we know the user has never answered. */
    readonly mustAsk = computed(() => this.loaded() && this.consent() === null);

    private readonly loadedFlag = signal(false);
    readonly loaded = this.loadedFlag.asReadonly();

    load(): void {
        this.userService.getMyPreferences().subscribe({
            next: (preferences) => {
                this.consent.set(preferences.analyticsConsent ?? null);
                this.loadedFlag.set(true);
            },
            error: (error) => {
                // Deliberately does not set `loaded`: without an answer we neither collect nor
                // nag. An unreachable backend must not be read as consent.
                console.error('Failed to load analytics consent', error);
            },
        });
    }

    /**
     * Persists the answer. Resolves once saved so the dialog can stay open on failure rather than
     * closing over an answer that was never stored.
     */
    save(granted: boolean): Promise<void> {
        return new Promise((resolve, reject) => {
            this.userService.getMyPreferences().subscribe({
                next: (current) => {
                    const next: UserPreferences = {...current, analyticsConsent: granted};

                    this.userService.updateMyPreferences(next).subscribe({
                        next: (saved) => {
                            // Trust the response rather than the optimistic value.
                            this.consent.set(saved.analyticsConsent ?? null);
                            this.loadedFlag.set(true);
                            resolve();
                        },
                        error: (error) => reject(error),
                    });
                },
                error: (error) => reject(error),
            });
        });
    }

    /** Called when the profile screen changes the flag, so the tracker sees it immediately. */
    set(granted: boolean | null): void {
        this.consent.set(granted);
        this.loadedFlag.set(true);
    }
}

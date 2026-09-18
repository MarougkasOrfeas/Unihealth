import {DestroyRef, inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {OAuthService} from 'angular-oauth2-oidc';
import {debounceTime, Observable, Subject} from 'rxjs';
import {BaseService} from '../../shared/services/base.service';
import {AnalyticsConsentService} from './analytics-consent.service';

/**
 * One measurement, matching the backend `LabelUsageDTO`.
 *
 * On the way out these are deltas to add; on the way back they are stored totals. The backend
 * reuses one DTO for both directions, so this mirrors it.
 */
export interface LabelUsage {
    labelCode: string;
    viewSeconds: number;
    interactionCount: number;
}

/** How long to let measurements pile up before writing them. */
const FLUSH_DEBOUNCE_MS = 30_000;

/**
 * A single view is capped here. Someone who opens a topic and walks away must not record an
 * afternoon. The backend clamps too — this is convenience, that is the control.
 */
const MAX_VIEW_MS = 10 * 60 * 1_000;

/**
 * Measures which health themes the user actually engages with, so recommendations can eventually
 * be tuned against behaviour rather than only against form answers.
 *
 * **Nothing happens without consent.** Every public method returns immediately unless consent is
 * explicitly `true` — the buffer is not even populated, so opting in later cannot retroactively
 * flush measurements taken while the answer was "no" or unknown.
 *
 * Cost is deliberately low: dwell is two `Date.now()` marks per view rather than a ticking timer
 * (following `AutoLogoutService`, which is wall-clock-anchored for the same reason — a tick
 * accumulator misreports badly on a throttled background tab), and writes are batched to roughly
 * one request per half-minute of activity.
 */
@Injectable({providedIn: 'root'})
export class UsageTrackingService {

    private static readonly ENDPOINT = `${BaseService.CONTEXT_PATH}/usage/_me`;

    private readonly httpClient = inject(HttpClient);
    private readonly oauthService = inject(OAuthService);
    private readonly consent = inject(AnalyticsConsentService);
    private readonly destroyRef = inject(DestroyRef);

    /** Label code to its pending, not-yet-written deltas. */
    private readonly pending = new Map<string, LabelUsage>();

    private readonly flushRequest = new Subject<void>();

    constructor() {
        this.flushRequest.pipe(
            debounceTime(FLUSH_DEBOUNCE_MS),
            takeUntilDestroyed(this.destroyRef),
        ).subscribe(() => this.flush(false));

        // `visibilitychange` and `pagehide` rather than `beforeunload`: the latter is unreliable on
        // mobile Safari, and three unsaved-form guards already use it — one of which calls
        // preventDefault() and would stall the flush.
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                this.flush(true);
            }
        });
        window.addEventListener('pagehide', () => this.flush(true));
    }

    /** Counts one engagement with each of the given labels. */
    trackInteraction(labelCodes: readonly string[]): void {
        this.accumulate(labelCodes, 0, 1);
    }

    /** Records time spent on content targeting the given labels. */
    trackView(labelCodes: readonly string[], elapsedMs: number): void {
        const clamped = Math.min(Math.max(0, elapsedMs), MAX_VIEW_MS);
        const seconds = Math.round(clamped / 1_000);

        if (seconds <= 0) {
            return;
        }
        this.accumulate(labelCodes, seconds, 0);
    }

    /**
     * The stored totals, for showing the user what has been recorded about them.
     *
     * Not gated on consent: withdrawal deletes the rows outright, so an unconsented account has
     * nothing to return anyway, and gating here would hide data from the very person it is about.
     */
    getMyUsage(): Observable<LabelUsage[]> {
        return this.httpClient.get<LabelUsage[]>(UsageTrackingService.ENDPOINT);
    }

    /** Drops anything buffered. Called when consent is withdrawn. */
    discard(): void {
        this.pending.clear();
    }

    private accumulate(labelCodes: readonly string[], seconds: number, interactions: number): void {
        if (!this.consent.granted() || labelCodes.length === 0) {
            return;
        }

        for (const code of labelCodes) {
            const current = this.pending.get(code)
                ?? {labelCode: code, viewSeconds: 0, interactionCount: 0};

            current.viewSeconds += seconds;
            current.interactionCount += interactions;
            this.pending.set(code, current);
        }

        this.flushRequest.next();
    }

    /**
     * @param unloading when true the page may be going away, so the request must survive teardown.
     *                  `HttpClient` calls get cancelled on unload, and `navigator.sendBeacon`
     *                  cannot carry the bearer token that `provideOAuthClient` normally injects —
     *                  hence `fetch` with `keepalive` and an explicit header.
     */
    private flush(unloading: boolean): void {
        if (!this.consent.granted() || this.pending.size === 0) {
            this.pending.clear();
            return;
        }

        const payload = [...this.pending.values()];
        // Cleared before the request, not after: a failed flush drops its measurements rather than
        // re-sending them forever. These are nice-to-have signals, not records worth retrying.
        this.pending.clear();

        if (unloading) {
            this.sendKeepalive(payload);
            return;
        }

        this.httpClient.post<void>(UsageTrackingService.ENDPOINT, payload).subscribe({
            error: (error) => console.error('Failed to send usage measurements', error),
        });
    }

    private sendKeepalive(payload: LabelUsage[]): void {
        try {
            const token = this.oauthService.getAccessToken();
            if (!token) {
                return;
            }

            void fetch(UsageTrackingService.ENDPOINT, {
                method: 'POST',
                keepalive: true,
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            }).catch(() => {
                // The page is leaving; there is nobody left to tell.
            });
        } catch {
            // Never let a measurement failure interfere with the page unloading.
        }
    }
}

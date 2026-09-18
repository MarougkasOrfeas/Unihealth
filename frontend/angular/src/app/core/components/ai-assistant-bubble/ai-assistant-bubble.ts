import {ChangeDetectionStrategy, Component, computed, inject, input, signal} from '@angular/core';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {NavigationEnd, Router, RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {filter, map, switchMap, tap, timer} from 'rxjs';

/** The page this bubble links to. The bubble hides itself while the user is already there. */
const CHAT_ROUTE = '/unihealth-ai';

/** Messages cycled through; each needs an `ai.bubble.message.<n>` lexicon key. */
const MESSAGE_COUNT = 10;
/** The greeting. Shown once per browser, then never again — see {@link nextIndex}. */
const WELCOME_INDEX = 0;

/** Let the page settle before the assistant speaks up. */
const FIRST_DELAY_MS = 3_000;
/** How long a message stays on screen. Long enough to read a full sentence without hurrying. */
const VISIBLE_MS = 30_000;
/** One message every five minutes: present, but not something you have to dismiss. */
const CYCLE_MS = 5 * 60 * 1_000;

const STORAGE_KEY = 'unihealth.aiBubble';

/** Persisted so the rotation resumes across reloads instead of restarting at the greeting. */
interface BubbleState {
    welcomeShown: boolean;
    /** Index of the message shown last, or -1 when nothing has been shown yet. */
    lastIndex: number;
}

/**
 * The floating assistant: a circular button carrying the university symbol, with a speech bubble
 * that cycles through short welcome and orientation messages.
 *
 * Rendered by the app shell as a sibling of the health-assistant FAB, stacked directly above it.
 * It deliberately does not live in a page component: `position: fixed` inside
 * `mat-sidenav-content` risks being captured by a transformed ancestor.
 *
 * **Order.** Strictly sequential, and the position is persisted, so a user who only stays a few
 * minutes still works through all ten messages over several visits rather than seeing the first
 * two forever. The greeting is shown once per browser; after a full pass the rotation restarts at
 * message 2, so nobody is welcomed twice.
 */
@Component({
    selector: 'app-ai-assistant-bubble',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [RouterLink, TranslatePipe],
    templateUrl: './ai-assistant-bubble.html',
    styleUrl: './ai-assistant-bubble.scss',
})
export class AiAssistantBubble {

    private readonly router = inject(Router);

    /** Set while the FAB's dialog is open — it opens over this same corner. */
    readonly isHidden = input(false);

    /** Bound in the template so the link target and the hide check cannot drift apart. */
    protected readonly chatRoute = CHAT_ROUTE;

    protected readonly index = signal(WELCOME_INDEX);
    protected readonly visible = signal(false);

    protected readonly messageKey = computed(() => `ai.bubble.message.${this.index() + 1}`);

    private readonly currentUrl = toSignal(
        this.router.events.pipe(
            filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            map((event) => event.urlAfterRedirects),
        ),
        {initialValue: this.router.url},
    );

    /** Pointless to invite someone to the chat page while they are reading it. */
    private readonly onAssistantPage = computed(() => this.currentUrl().startsWith(CHAT_ROUTE));

    protected readonly suppressed = computed(() => this.isHidden() || this.onAssistantPage());

    private state: BubbleState = AiAssistantBubble.readState();

    constructor() {
        timer(FIRST_DELAY_MS, CYCLE_MS).pipe(
            // Nothing advances while the bubble cannot be seen, so sitting on the chat page or
            // keeping the dialog open does not burn through the orientation messages unread.
            filter(() => !this.suppressed()),
            tap(() => this.showNext()),
            // The inner timer always completes first (VISIBLE_MS < CYCLE_MS), so switchMap never
            // cancels it mid-flight; it just ends the visible phase of each cycle.
            switchMap(() => timer(VISIBLE_MS).pipe(tap(() => this.visible.set(false)))),
            takeUntilDestroyed(),
        ).subscribe();
    }

    private showNext(): void {
        const next = AiAssistantBubble.nextIndex(this.state);

        this.index.set(next);
        this.visible.set(true);

        this.state = {
            welcomeShown: this.state.welcomeShown || next === WELCOME_INDEX,
            lastIndex: next,
        };
        AiAssistantBubble.writeState(this.state);
    }

    /**
     * The next message in sequence, skipping the greeting once it has been seen. A completed pass
     * restarts at the message after the greeting rather than wrapping to it.
     */
    private static nextIndex(state: BubbleState): number {
        const candidate = state.lastIndex + 1;

        if (candidate >= MESSAGE_COUNT) {
            return WELCOME_INDEX + 1;
        }
        if (candidate === WELCOME_INDEX && state.welcomeShown) {
            return WELCOME_INDEX + 1;
        }
        return candidate;
    }

    /** Tolerant of absent, malformed or stale values — this is decorative state, never critical. */
    private static readState(): BubbleState {
        const fallback: BubbleState = {welcomeShown: false, lastIndex: -1};

        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) {
                return fallback;
            }

            const parsed = JSON.parse(raw) as Partial<BubbleState>;
            const lastIndex = parsed.lastIndex;

            return {
                welcomeShown: parsed.welcomeShown === true,
                // Guards against a stored index left over from a shorter message list.
                lastIndex: typeof lastIndex === 'number'
                    && Number.isInteger(lastIndex)
                    && lastIndex >= 0
                    && lastIndex < MESSAGE_COUNT
                    ? lastIndex
                    : -1,
            };
        } catch {
            // localStorage throws in private mode in some browsers.
            return fallback;
        }
    }

    private static writeState(state: BubbleState): void {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
        } catch {
            // Losing the position only means the rotation restarts; not worth surfacing.
        }
    }
}

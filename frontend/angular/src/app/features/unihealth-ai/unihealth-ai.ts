import {
    Component,
    DestroyRef,
    ElementRef,
    Injector,
    OnInit,
    afterNextRender,
    computed,
    inject,
    signal,
    viewChild,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {TranslatePipe} from '@ngx-translate/core';
import {Subscription, filter, finalize, switchMap} from 'rxjs';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';
import {MessageService} from '../../shared/services/message.service';
import {UnihealthAiService} from '../../shared/services/unihealth-ai.service';
import {
    AiConversationDetailDto,
    AiConversationDto,
    AiPendingActionDto,
    ChatMessage,
    UnihealthAiChatResponseDto,
} from '../../shared/interfaces/unihealth-ai';
import {ChatHistory} from './chat-history/chat-history';

/**
 * What a brand-new chat starts with.
 *
 * A function rather than a shared constant, so two new chats never hand out the same object — and
 * so reopening an old conversation can *replace* this list rather than add to it.
 */
function newChat(): ChatMessage[] {
    // Seeded as a key, not as text: this greeting is ours, not the model's.
    return [{role: 'assistant', contentKey: 'ai.chat.greeting', createdAt: new Date()}];
}

@Component({
    selector: 'app-unihealth-ai',
    standalone: true,
    imports: [FormsModule, TranslatePipe, ChatHistory],
    templateUrl: './unihealth-ai.html',
    styleUrls: ['./unihealth-ai.scss'],
})
export class UnihealthAi implements OnInit {
    private readonly unihealthAiService = inject(UnihealthAiService);
    private readonly messageService = inject(MessageService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly injector = inject(Injector);

    private readonly messagesPanel = viewChild<ElementRef<HTMLElement>>('messagesPanel');

    /**
     * The turn in flight, held so that leaving a conversation can cancel it.
     *
     * `takeUntilDestroyed` alone is not enough here: it ends the stream when the *component* goes
     * away, and switching conversation does not destroy the component.
     */
    private turn: Subscription | null = null;

    readonly message = signal('');
    readonly loading = signal(false);
    /** A lexicon key rather than text, so the message follows a language switch. */
    readonly errorKey = signal('');

    /**
     * The change waiting for an answer, if any.
     *
     * Held here as well as on the message so the composer can be locked while it is open: leaving
     * the student free to ask something else would let a proposal sit around unanswered until it
     * quietly expired.
     */
    readonly pendingAction = signal<AiPendingActionDto | null>(null);
    readonly confirming = signal(false);

    readonly messages = signal<ChatMessage[]>(newChat());

    /** The caller's past chats, newest first. Empty until the first turn is recorded. */
    readonly conversations = signal<AiConversationDto[]>([]);

    /** The conversation on screen, or null for a new chat that has not been recorded yet. */
    readonly conversationId = signal<string | null>(null);

    readonly historyLoading = signal(false);

    /** Off-canvas drawer state. Only has an effect below 900px, where the rail is not in flow. */
    readonly historyOpen = signal(false);

    /**
     * Lexicon key of whatever the assistant is doing right now, or empty when it is idle.
     *
     * A turn takes several seconds on the hardware this runs on, and the stages are reported by the
     * server as they actually happen — so «Για να δω τι λένε και οι επίσημες πηγές...» appears only
     * on the turns that really did go outside. Animating these on a timer would be quicker to build
     * and would be a lie.
     */
    readonly stageKey = signal('');

    readonly canSend = computed(
        () => !this.loading() && !this.pendingAction() && this.message().trim().length > 0,
    );

    ngOnInit(): void {
        this.loadConversations();
    }

    sendMessage(): void {
        const trimmedMessage = this.message().trim();

        if (!trimmedMessage || this.loading() || this.pendingAction()) {
            return;
        }

        this.errorKey.set('');

        this.messages.update((messages) => [
            ...messages,
            {
                role: 'user',
                content: trimmedMessage,
                createdAt: new Date(),
            },
        ]);

        this.loading.set(true);
        this.stageKey.set('ai.stage.thinking');

        // The input is cleared as the request leaves, not when it returns: the question is already
        // on screen above, and leaving it in the box invites a second send during the wait.
        this.message.set('');
        this.scrollToLatest();

        // Null means "start a new chat". The server mints the id and hands it back on the answer
        // line; it is never chosen here, and never taken as proof of whose conversation this is.
        this.turn = this.unihealthAiService
            .chatStream({
                message: trimmedMessage,
                conversationId: this.conversationId() ?? undefined,
            })
            .pipe(
                finalize(() => {
                    this.loading.set(false);
                    this.stageKey.set('');
                }),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: (line) => {
                    if (line.type === 'stage') {
                        this.stageKey.set(line.key);
                        return;
                    }

                    if (line.type === 'error') {
                        this.errorKey.set(line.key);
                        return;
                    }

                    this.appendAnswer(line.payload);
                },
                error: () => this.errorKey.set('ai.chat.error'),
            });
    }

    private appendAnswer(response: UnihealthAiChatResponseDto): void {
        // A screened message comes back as a lexicon key with no reply: the backend never called
        // the model. Rendering it as a key rather than as text is what keeps the crisis wording
        // reviewed and following the language switch.
        const screened = !!response.safetyKey;

        this.messages.update((messages) => [
            ...messages,
            {
                role: 'assistant',
                content: screened ? undefined : response.reply,
                contentKey: response.safetyKey,
                safety: screened,
                citations: response.citations ?? [],
                pendingAction: response.pendingAction,
                createdAt: new Date(),
            },
        ]);

        this.pendingAction.set(response.pendingAction ?? null);
        this.scrollToLatest();

        // Absent on a screened turn, because nothing was stored — that is the whole storage rule,
        // and it is why this must not fall back to the id the request was sent with.
        if (response.conversationId) {
            this.conversationId.set(response.conversationId);

            // The rail sorts on last-message time, so an existing chat has just moved to the top
            // and a new one did not exist a moment ago. One cheap query after a ten-second turn.
            this.loadConversations();
        }
    }

    // --- History ------------------------------------------------------------

    /**
     * Opens a past conversation.
     *
     * <b>The cancel below is the point of this method.</b> A turn already in flight keeps writing
     * into `messages()` until it is stopped, so without it the answer to the question just asked
     * arrives inside whichever conversation the student switched to.
     */
    selectConversation(id: string): void {
        this.historyOpen.set(false);

        if (id === this.conversationId()) {
            return;
        }

        this.abandonTurn();
        this.errorKey.set('');

        this.unihealthAiService
            .findMyConversation(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (conversation) => this.restore(conversation),
                error: () => this.errorKey.set('ai.chat.error'),
            });
    }

    startNewChat(): void {
        this.historyOpen.set(false);
        this.abandonTurn();

        this.errorKey.set('');
        this.conversationId.set(null);
        this.messages.set(newChat());
    }

    deleteConversation(conversation: AiConversationDto): void {
        this.messageService
            .confirmDelete(UNIHEALTH_CONSTANTS.ENTITY.CONVERSATION)
            .afterClosed()
            .pipe(
                filter(Boolean),
                switchMap(() => this.unihealthAiService.deleteMyConversation(conversation.id)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: () => {
                    this.messageService.deleteSuccess(UNIHEALTH_CONSTANTS.ENTITY.CONVERSATION);

                    // Deleting the conversation on screen would otherwise leave the page showing a
                    // transcript that no longer exists, and sending the next turn into a dead id.
                    if (conversation.id === this.conversationId()) {
                        this.startNewChat();
                    }

                    this.loadConversations();
                },
                error: () => this.messageService.deleteError(),
            });
    }

    private loadConversations(): void {
        this.historyLoading.set(true);

        this.unihealthAiService
            .findMyConversations()
            .pipe(
                finalize(() => this.historyLoading.set(false)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: (conversations) => this.conversations.set(conversations),
                // Deliberately not surfaced. The rail is a convenience and the chat works without
                // it; a banner over the answer would be shouting about the wrong thing.
                error: () => this.conversations.set([]),
            });
    }

    private restore(conversation: AiConversationDetailDto): void {
        this.conversationId.set(conversation.id);

        // set, not update. The greeting is seeded when the component is created, so appending here
        // would open every reopened chat with a greeting that conversation never had.
        this.messages.set(
            conversation.messages.map((message): ChatMessage => ({
                role: message.role === 'USER' ? 'user' : 'assistant',
                content: message.content,
                citations: message.citations ?? [],
                createdAt: new Date(message.createdOn),
            })),
        );

        // No pendingAction is restored, and none is stored. A proposal lives in Redis behind a
        // short expiry, so a confirmation button for an actionId from last week is a button that
        // can only fail.
        this.scrollToLatest();
    }

    private abandonTurn(): void {
        // finalize on the subscription clears the spinner and the stage line as this unwinds.
        this.turn?.unsubscribe();
        this.turn = null;

        // Or the composer stays locked, holding an actionId that belongs to a conversation the
        // student has just left.
        this.clearPendingAction();
    }

    /**
     * Pins the transcript to its latest message.
     *
     * `.chat-messages` is a plain overflow box with no scroll handling of its own, so a restored
     * conversation of thirty messages would otherwise open at the top, showing the oldest thing
     * the student said. After render rather than after the signal: the rows do not exist yet.
     */
    private scrollToLatest(): void {
        afterNextRender(
            () => {
                const panel = this.messagesPanel()?.nativeElement;

                if (panel) {
                    panel.scrollTop = panel.scrollHeight;
                }
            },
            {injector: this.injector},
        );
    }

    // --- Confirmation cards -------------------------------------------------

    /**
     * Sends the student's answer to a confirmation card.
     *
     * The outcome is appended as a lexicon key rather than as text, because the server decides what
     * happened — applied, expired, or refused because the settings moved in another tab — and the
     * model has no part in saying so.
     */
    resolveAction(approved: boolean): void {
        const action = this.pendingAction();

        if (!action || this.confirming()) {
            return;
        }

        this.errorKey.set('');
        this.confirming.set(true);

        this.unihealthAiService
            .confirmAction({actionId: action.actionId, approved})
            .pipe(finalize(() => this.confirming.set(false)))
            .subscribe({
                next: (outcome) => {
                    this.clearPendingAction();

                    this.messages.update((messages) => [
                        ...messages,
                        {
                            role: 'assistant',
                            contentKey: outcome.messageKey,
                            createdAt: new Date(),
                        },
                    ]);

                    this.scrollToLatest();
                },
                error: () => {
                    // The card goes either way. Leaving it on screen after a failed call invites a
                    // second click on an actionId the server may already have consumed.
                    this.clearPendingAction();
                    this.errorKey.set('ai.action.failed');
                },
            });
    }

    private clearPendingAction(): void {
        this.pendingAction.set(null);

        this.messages.update((messages) =>
            messages.map((message) =>
                message.pendingAction ? {...message, pendingAction: undefined} : message,
            ),
        );
    }
}

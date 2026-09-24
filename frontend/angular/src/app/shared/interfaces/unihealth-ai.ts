export interface UnihealthAiChatRequestDto {
    message: string;
    /**
     * The chat to continue, or omitted to start a new one.
     *
     * Never an identity claim: the server loads it by (id, owner) and treats a conversation
     * belonging to somebody else as simply absent.
     */
    conversationId?: string;
}

/**
 * One passage the answer was grounded in.
 *
 * Built on the server from document metadata and never written by the model, so a link here is
 * always a real link — the one kind of hallucination a citation exists to rule out.
 */
export interface AiCitationDto {
    title: string;
    sectionLabel: string;
    sourceName: string;
    /** Empty for the university's own content, which has no public page to link to. */
    sourceUrl: string;
    /** Urgent and emergency sections, surfaced by rule rather than by relevance. */
    urgent: boolean;
    /**
     * `LOCAL_VETTED` for curated, licensed content; `LIVE_WEB` for a page fetched from a health
     * authority during this turn. The two are not the same kind of claim and must stay visually
     * distinct — that distinction is the whole reason the field exists.
     */
    sourceType: 'LOCAL_VETTED' | 'LIVE_WEB' | string;
    /** ISO date the external item was published. Empty for local content. */
    publishedAt: string;
    /** ISO instant the page was fetched. Empty for local content. */
    retrievedAt: string;
}

/** One before-and-after line on a confirmation card. `labelKey` is a lexicon key, not a label. */
export interface AiActionFieldChangeDto {
    labelKey: string;
    from: boolean;
    to: boolean;
}

/**
 * A change the assistant has proposed and the student has not yet approved.
 *
 * Render `changes` literally and trust it over the reply text: a small model will sometimes say it
 * has already made a change it has only proposed. Nothing is written until `actionId` comes back on
 * `/ai/chat/confirm`.
 */
export interface AiPendingActionDto {
    actionId: string;
    kind: string;
    labelKey: string;
    changes: AiActionFieldChangeDto[];
    /** Confirming destroys data — withdrawing analytics consent also deletes collected metrics. */
    irreversible: boolean;
}

export interface UnihealthAiChatResponseDto {
    /** The generated answer. Absent when `safetyKey` is set. */
    reply?: string;
    /**
     * A lexicon key to render instead of `reply`, set when the backend screened the message as a
     * crisis and never called the model. The wording is reviewed and fixed, which is the whole
     * point: it must not vary between turns and must not be paraphrased.
     */
    safetyKey?: string;
    citations: AiCitationDto[];
    /** False when the answer came from the model's own training data rather than our library. */
    grounded: boolean;
    pendingAction?: AiPendingActionDto;
    /**
     * The chat this turn was filed under, so a client that started a new one learns its id.
     *
     * Absent when nothing was stored, which is every screened turn: a message the backend never
     * put to the model is answered and then forgotten, and on a brand-new chat that means no
     * conversation is created at all.
     */
    conversationId?: string;
}

/**
 * One line of the streamed turn.
 *
 * `stage` lines arrive as each stage begins and carry a lexicon key, never text. The final line is
 * either `answer` or `error`.
 */
export type AiStreamLine =
    | { type: 'stage'; key: string }
    | { type: 'answer'; payload: UnihealthAiChatResponseDto }
    | { type: 'error'; key: string };

export interface AiConfirmActionRequestDto {
    actionId: string;
    approved: boolean;
}

/** What the server did about a confirmation. `messageKey` is a lexicon key. */
export interface AiActionOutcomeDto {
    messageKey: string;
    applied: boolean;
}

export interface ChatMessage {
    role: 'user' | 'assistant';
    /** Literal text: a real reply from the model, or something the user typed. */
    content?: string;
    /**
     * Lexicon key, for messages the app itself seeds such as the opening greeting. Takes
     * precedence over `content` and is resolved in the template, so seeded text follows a
     * language switch instead of freezing in whichever locale was active at load.
     */
    contentKey?: string;
    /**
     * Marks a crisis response so the bubble can carry emergency styling. A student scanning the
     * page for a phone number should not have to read a paragraph to find one.
     */
    safety?: boolean;
    /** Sources shown under the reply. Empty when the answer was not grounded. */
    citations?: AiCitationDto[];
    /** Set while this turn's proposed change is still awaiting an answer. */
    pendingAction?: AiPendingActionDto;
    createdAt: Date;
}

/**
 * One row in the history rail.
 *
 * `lastMessageOn` is denormalised on the server so the list sorts on one indexed column — the
 * rail never loads a conversation's messages just to find out how recent it is.
 */
export interface AiConversationDto {
    id: string;
    /**
     * What the chat is about — the symptom its first question named, or that question trimmed
     * when it named none. Decided on the server and never model-generated, so it is already
     * short enough for the rail and needs no client-side truncation.
     */
    title: string;
    /** ISO local date-time. */
    lastMessageOn: string;
}

/** One stored message. Only `USER` and `ASSISTANT` are kept; system and tool turns are not. */
export interface AiMessageDto {
    role: 'USER' | 'ASSISTANT' | string;
    content: string;
    citations: AiCitationDto[];
    grounded: boolean;
    /** ISO local date-time. */
    createdOn: string;
}

/** A conversation with its messages, in order. */
export interface AiConversationDetailDto {
    id: string;
    title: string;
    messages: AiMessageDto[];
}

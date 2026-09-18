export interface UnihealthAiChatRequestDto {
    message: string;
}

export interface UnihealthAiChatResponseDto {
    reply: string;
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
    createdAt: Date;
}

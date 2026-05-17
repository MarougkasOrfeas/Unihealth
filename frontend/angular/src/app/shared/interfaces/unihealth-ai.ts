export interface UnihealthAiChatRequestDto {
    message: string;
}

export interface UnihealthAiChatResponseDto {
    reply: string;
}

export interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    createdAt: Date;
}

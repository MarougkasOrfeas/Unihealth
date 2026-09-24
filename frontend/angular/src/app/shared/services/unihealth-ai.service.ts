import {Injectable, inject} from '@angular/core';
import {HttpClient, HttpEventType} from '@angular/common/http';
import {Observable, concatMap, defer, filter, from, map} from 'rxjs';
import {
    AiActionOutcomeDto,
    AiConfirmActionRequestDto,
    AiConversationDetailDto,
    AiConversationDto,
    AiStreamLine,
    UnihealthAiChatRequestDto,
    UnihealthAiChatResponseDto,
} from "../interfaces/unihealth-ai";


@Injectable({
    providedIn: 'root',
})
export class UnihealthAiService {
    private readonly http = inject(HttpClient);

    private readonly baseUrl = '/api/ai';

    /**
     * A separate controller, not a sub-path of `/ai`.
     *
     * Every endpoint under it is `_me` scoped: the server resolves whose conversations these are
     * from the bearer token and never from the request, so there is no id here that names a
     * student. That is why none of the methods below takes a user.
     */
    private readonly conversationUrl = '/api/ai-conversation';

    chat(request: UnihealthAiChatRequestDto): Observable<UnihealthAiChatResponseDto> {
        return this.http.post<UnihealthAiChatResponseDto>(`${this.baseUrl}/chat`, request);
    }

    /**
     * The same turn, but reporting each stage as it starts.
     *
     * Uses `reportProgress` with a text response rather than `EventSource`, for one practical
     * reason: `EventSource` cannot send an `Authorization` header, and every endpoint here sits
     * behind a Keycloak bearer token. Going through `HttpClient` keeps the existing OAuth
     * interceptor, so the stream is authenticated exactly like every other call.
     *
     * Angular hands us the whole body received so far on each progress event, not the delta, so
     * the parser below tracks how much it has already emitted.
     */
    chatStream(request: UnihealthAiChatRequestDto): Observable<AiStreamLine> {
        // defer, so the cursor below belongs to each subscription rather than to the service. A
        // shared cursor would make a second subscriber silently skip the lines the first consumed.
        return defer(() => {
            let consumedChars = 0;

            return this.http
                .post(`${this.baseUrl}/chat/stream`, request, {
                    observe: 'events',
                    responseType: 'text',
                    reportProgress: true,
                })
                .pipe(
                    map((event) => {
                        if (event.type === HttpEventType.DownloadProgress) {
                            return (event as { partialText?: string }).partialText ?? '';
                        }
                        if (event.type === HttpEventType.Response) {
                            return event.body ?? '';
                        }
                        return null;
                    }),
                    filter((body): body is string => body !== null),
                    map((body) => {
                        const {lines, consumed} = this.linesFrom(body, consumedChars);
                        consumedChars = consumed;
                        return lines;
                    }),
                    // One emission per line rather than per progress notification: a single flush
                    // can carry several lines, and a partial line waits for the rest of itself.
                    concatMap((lines) => from(lines)),
                );
        });
    }

    /**
     * Splits off only the complete lines that have not been emitted yet.
     *
     * The trailing fragment is deliberately left unconsumed: a flush can land mid-object, and
     * parsing half of one would throw on perfectly healthy input.
     */
    private linesFrom(
        body: string,
        alreadyConsumed: number,
    ): { lines: AiStreamLine[]; consumed: number } {
        const lastBreak = body.lastIndexOf('\n');

        if (lastBreak < alreadyConsumed) {
            return {lines: [], consumed: alreadyConsumed};
        }

        const lines = body
            .slice(alreadyConsumed, lastBreak)
            .split('\n')
            .map((line) => line.trim())
            .filter((line) => line.length > 0)
            .map((line) => this.parse(line))
            .filter((line): line is AiStreamLine => line !== null);

        return {lines, consumed: lastBreak + 1};
    }

    private parse(line: string): AiStreamLine | null {
        try {
            return JSON.parse(line) as AiStreamLine;
        } catch {
            // A malformed line should cost that line, not the turn. The answer may still be coming.
            return null;
        }
    }

    /**
     * Answers a confirmation card.
     *
     * A separate endpoint on purpose: this is the only call in the feature that changes anything,
     * and it is reached because a person clicked, never because the model produced a sentence.
     */
    confirmAction(request: AiConfirmActionRequestDto): Observable<AiActionOutcomeDto> {
        return this.http.post<AiActionOutcomeDto>(`${this.baseUrl}/chat/confirm`, request);
    }

    /** The caller's own conversations, most recently used first. The server caps `limit` at 50. */
    findMyConversations(limit = 30): Observable<AiConversationDto[]> {
        return this.http.get<AiConversationDto[]>(`${this.conversationUrl}/_me`, {
            params: {limit},
        });
    }

    /**
     * One conversation with its messages.
     *
     * Answers 404 for a conversation belonging to somebody else — the server loads it by
     * (id, owner), so a row that is not yours is simply not found rather than forbidden.
     */
    findMyConversation(id: string): Observable<AiConversationDetailDto> {
        return this.http.get<AiConversationDetailDto>(`${this.conversationUrl}/_me/${id}`);
    }

    deleteMyConversation(id: string): Observable<void> {
        return this.http.delete<void>(`${this.conversationUrl}/_me/${id}`);
    }

    /** The "delete everything you hold on me" half of the pair, as offered for usage metrics. */
    deleteMyConversations(): Observable<void> {
        return this.http.delete<void>(`${this.conversationUrl}/_me`);
    }
}

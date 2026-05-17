import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UnihealthAiChatRequestDto, UnihealthAiChatResponseDto} from "../interfaces/unihealth-ai";


@Injectable({
    providedIn: 'root',
})
export class UnihealthAiService {
    private readonly http = inject(HttpClient);

    private readonly baseUrl = '/api/ai';

    chat(request: UnihealthAiChatRequestDto): Observable<UnihealthAiChatResponseDto> {
        return this.http.post<UnihealthAiChatResponseDto>(`${this.baseUrl}/chat`, request);
    }
}

import {Component, computed, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TranslatePipe} from '@ngx-translate/core';
import {finalize} from 'rxjs';
import {UnihealthAiService} from '../../shared/services/unihealth-ai.service';
import {ChatMessage} from '../../shared/interfaces/unihealth-ai';

@Component({
    selector: 'app-unihealth-ai',
    standalone: true,
    imports: [FormsModule, TranslatePipe],
    templateUrl: './unihealth-ai.html',
    styleUrls: ['./unihealth-ai.scss'],
})
export class UnihealthAi {
    private readonly unihealthAiService = inject(UnihealthAiService);

    readonly message = signal('');
    readonly loading = signal(false);
    /** A lexicon key rather than text, so the message follows a language switch. */
    readonly errorKey = signal('');

    // Seeded as a key, not as text: this greeting is ours, not the model's.
    readonly messages = signal<ChatMessage[]>([
        {
            role: 'assistant',
            contentKey: 'ai.chat.greeting',
            createdAt: new Date(),
        },
    ]);

    readonly canSend = computed(
        () => !this.loading() && this.message().trim().length > 0,
    );

    sendMessage(): void {
        const trimmedMessage = this.message().trim();

        if (!trimmedMessage || this.loading()) {
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

        this.unihealthAiService
            .chat({message: trimmedMessage})
            .pipe(finalize(() => this.loading.set(false)))
            .subscribe({
                next: (response) => {
                    this.messages.update((messages) => [
                        ...messages,
                        {
                            role: 'assistant',
                            content: response.reply,
                            createdAt: new Date(),
                        },
                    ]);

                    this.message.set('');
                },
                error: () => this.errorKey.set('ai.chat.error'),
            });
    }
}

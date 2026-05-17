import {Component, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {UnihealthAiService} from '../../shared/services/unihealth-ai.service';
import {ChatMessage} from '../../shared/interfaces/unihealth-ai';

@Component({
    selector: 'app-unihealth-ai',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './unihealth-ai.html',
    styleUrls: ['./unihealth-ai.scss'],
})
export class UnihealthAi {
    private readonly unihealthAiService = inject(UnihealthAiService);

    readonly message = signal('');
    readonly loading = signal(false);
    readonly errorMessage = signal('');

    readonly messages = signal<ChatMessage[]>([
        {
            role: 'assistant',
            content:
                'Hi, I’m UniHealth. You can ask me about student wellbeing, sleep, stress, exercise, nutrition, and general health guidance.',
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

        this.errorMessage.set('');

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
                error: () => {
                    this.errorMessage.set(
                        'Something went wrong while contacting UniHealth AI.',
                    );
                },
            });
    }

    trackByIndex(index: number): number {
        return index;
    }
}

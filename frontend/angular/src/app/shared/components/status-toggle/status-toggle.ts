import {ChangeDetectionStrategy, Component, computed, inject, input, output} from '@angular/core';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {filter, take} from 'rxjs';
import {Button} from '../button/button';
import {SectionTitle} from '../section-title/section-title';
import {MessageService} from '../../services/message.service';

/**
 * The states this control can display. The values double as the CSS class names of the status pill
 * in `styles.scss`.
 *
 * `unverified` applies to users only: the account is enabled in Keycloak but the email has not been
 * confirmed yet. It is a pending state, not a switched-off one.
 */
export type EntityStatus = 'active' | 'inactive' | 'unverified';

/**
 * Activate / deactivate control for an entity's edit page.
 *
 * The status is not part of the form: the backend ignores `active` on update and exposes a
 * dedicated endpoint per entity, so this acts immediately after confirmation rather than waiting
 * for Save. The caller decides who sees it.
 */
@Component({
    selector: 'app-status-toggle',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [Button, TranslatePipe, SectionTitle],
    templateUrl: './status-toggle.html',
    styleUrl: './status-toggle.scss',
})
export class StatusToggle {

    private readonly messages = inject(MessageService);
    private readonly translate = inject(TranslateService);

    readonly status = input.required<EntityStatus>();
    /** Lexicon key of the entity noun, interpolated into the confirmation text. */
    readonly entityKey = input.required<string>();
    readonly disabled = input<boolean>(false);
    /** Lexicon key explaining why the control is disabled, shown as a tooltip. */
    readonly disabledReason = input<string>('');
    readonly testId = input<string>('status-toggle');

    /** Emits the requested state once the user confirms. */
    readonly statusChange = output<boolean>();

    protected readonly statusKey = computed(() => {
        switch (this.status()) {
            case 'active':
                return 'global.status.active';
            case 'unverified':
                return 'global.status.unverified';
            default:
                return 'global.status.inactive';
        }
    });

    /**
     * Whether the action deactivates. An unverified account is already enabled, so the only
     * meaningful action on it is switching it off — "activate" would be a no-op that reads as a
     * failure, because the backend keeps it UNVERIFIED until the email is confirmed.
     */
    protected readonly deactivates = computed(() => this.status() !== 'inactive');

    protected readonly actionKey = computed(
        () => this.deactivates() ? 'global.action.deactivate' : 'global.action.activate',
    );

    protected onToggle(): void {
        if (this.disabled()) {
            return;
        }

        const next = !this.deactivates();
        const prefix = next ? 'global.activate.confirm' : 'global.deactivate.confirm';
        // Title and content are interpolated here because BaseDialog applies `| translate` with no
        // params, so a raw key would render "{{entity}}" literally. Already-resolved text passes
        // through the pipe unchanged. Confirm/cancel stay keys — the template translates those.
        const entity = this.translate.instant(this.entityKey());

        this.messages.confirm({
            title: this.translate.instant(`${prefix}.title`, {entity}),
            content: this.translate.instant(`${prefix}.content`, {entity}),
            confirmText: this.actionKey(),
            cancelText: 'global.cancel',
            isDestructive: !next,
        })
            .afterClosed()
            .pipe(filter(Boolean), take(1))
            .subscribe(() => this.statusChange.emit(next));
    }
}

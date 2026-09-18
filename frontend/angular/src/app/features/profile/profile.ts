import {Component, DestroyRef, inject, OnInit, signal} from "@angular/core";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router, RouterLink} from "@angular/router";
import {DatePipe, NgIf} from "@angular/common";
import {UserService} from "../../shared/services/user.service";
import {User, UserPreferences} from "../../shared/interfaces/user";
import {AuthService} from "../../core/auth/auth.service";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {finalize, take} from "rxjs";
import {MessageService} from "../../shared/services/message.service";
import {AnalyticsConsentService} from "../../core/services/analytics-consent.service";

@Component({
    selector: "app-profile",
    standalone: true,
    imports: [
        ReactiveFormsModule,
        NgIf,
        DatePipe,
        TranslatePipe,
        RouterLink
    ],
    templateUrl: "./profile.html",
    styleUrl: "./profile.scss",
})
export class Profile implements OnInit {

    private fb = inject(FormBuilder);
    private router = inject(Router);
    private userService = inject(UserService);
    private consent = inject(AnalyticsConsentService);
    protected userId!: string;
    user: User | null = null;
    editMode = false;
    private authService = inject(AuthService);
    private readonly messages = inject(MessageService);
    private readonly translate = inject(TranslateService);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * What a brand-new account gets: both opted in. Also the fallback when the preferences cannot
     * be loaded, so a failed request leaves the switches usable instead of permanently disabled.
     */
    private static readonly DEFAULT_PREFERENCES: UserPreferences = {
        newsletterSubscribed: true,
        notificationsEnabled: true,
        // Never defaulted to true: measurement requires an explicit answer, and `null` renders the
        // switch off without claiming the user declined.
        analyticsConsent: null,
    };

    /** Never null: the switches always have something coherent to render. */
    readonly preferences = signal<UserPreferences>({...Profile.DEFAULT_PREFERENCES});
    readonly loadingPreferences = signal(true);
    readonly savingPreferences = signal(false);

    form = this.fb.group({
        firstname: ['', Validators.required],
        lastname: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]]
    });

    ngOnInit(): void {
        this.loadUser();
        this.loadPreferences();
    }

    private loadUser(): void {
        this.userService.getUser().subscribe({
            next: (user) => {
                this.user = user;

                this.form.patchValue({
                    firstname: user.firstname,
                    lastname: user.lastname,
                    email: user.email
                });
            },
            error: () => {
                console.error("Failed to load user");
            }
        });
    }

    onEdit(): void {
        this.editMode = true;
    }

    onSave(): void {

        if (this.form.invalid || !this.user) {
            this.form.markAllAsTouched();
            return;
        }

        const value = this.form.getRawValue();

        const updatedUser = {
            ...this.user,
            firstname: value.firstname ?? '',
            lastname: value.lastname ?? '',
            email: value.email ?? ''
        };

        this.editMode = false;

        this.userService.update(this.user.id, updatedUser).subscribe({
            next: () => {
                this.user = updatedUser;
                this.editMode = false;
            },
            error: (err) => {
                console.error('Failed to update profile', err);
            }
        });
    }

    onCancel(): void {
        this.editMode = false;

        if (!this.user) return;

        this.form.patchValue({
            firstname: this.user.firstname,
            lastname: this.user.lastname,
            email: this.user.email
        });
    }

    onEditHealthForm(): void {
        this.router.navigate(['/profile/form']);
    }

    onLogout(): void {
        this.authService.logout();
    }

    private loadPreferences(): void {
        this.userService.getMyPreferences()
            .pipe(
                // finalize, not the callbacks: the flag has to clear on every outcome. Clearing it
                // only in next/error is what left the switches permanently disabled once.
                finalize(() => this.loadingPreferences.set(false)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: (preferences) => this.preferences.set(preferences),
                // Keep the defaults rather than leaving the switches dead. A save attempt will
                // surface the real error, which says more than a control that does nothing.
                error: () => this.messages.error(
                    this.translate.instant('profile.preferences.load.error')),
            });
    }

    /**
     * The switch reads "unsubscribe", the stored value reads "subscribed" — checked means
     * unsubscribed, so the value is inverted here.
     */
    onUnsubscribeToggle(event: Event): void {
        const input = event.target as HTMLInputElement;

        if (input.checked) {
            // Only the opt-out is confirmed: it stops mail and sends the goodbye email, whereas
            // opting back in is harmless.
            this.messages.confirm({
                title: this.translate.instant('profile.preferences.unsubscribe.confirm.title'),
                content: this.translate.instant('profile.preferences.unsubscribe.confirm.content'),
                confirmText: 'profile.preferences.unsubscribe.confirm.confirm',
                cancelText: 'global.cancel',
                isDestructive: true,
            }).afterClosed().pipe(take(1)).subscribe((confirmed) => {
                if (confirmed) {
                    this.savePreferences({newsletterSubscribed: false}, input);
                    return;
                }
                // Declined: put the switch back, since the browser already flipped it.
                input.checked = !this.preferences().newsletterSubscribed;
            });
            return;
        }

        this.savePreferences({newsletterSubscribed: true}, input);
    }

    /**
     * Withdrawal is confirmed because it is destructive: the backend deletes every measurement
     * already collected, not merely stops collecting.
     */
    onAnalyticsToggle(event: Event): void {
        const input = event.target as HTMLInputElement;

        if (!input.checked) {
            this.messages.confirm({
                title: this.translate.instant('profile.preferences.analytics.withdraw.confirm.title'),
                content: this.translate.instant('profile.preferences.analytics.withdraw.confirm.content'),
                confirmText: 'profile.preferences.analytics.withdraw.confirm.confirm',
                cancelText: 'global.cancel',
                isDestructive: true,
            }).afterClosed().pipe(take(1)).subscribe((confirmed) => {
                if (confirmed) {
                    this.savePreferences({analyticsConsent: false}, input);
                    return;
                }
                input.checked = this.preferences().analyticsConsent === true;
            });
            return;
        }

        this.savePreferences({analyticsConsent: true}, input);
    }

    onNotificationsToggle(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.savePreferences({notificationsEnabled: input.checked}, input);
    }

    /**
     * @param change the single preference the user just flipped.
     * @param input  the checkbox behind it, so a failed save can be rolled back visually. Resetting
     *               the signal would not do it: the bound boolean is unchanged, so Angular sees
     *               nothing to re-render while the DOM already shows the new position.
     */
    private savePreferences(change: Partial<UserPreferences>, input: HTMLInputElement): void {
        if (this.savingPreferences()) {
            return;
        }

        const previous = this.preferences();
        const next: UserPreferences = {...previous, ...change};
        this.savingPreferences.set(true);

        this.userService.updateMyPreferences(next)
            .pipe(
                finalize(() => this.savingPreferences.set(false)),
                takeUntilDestroyed(this.destroyRef),
            )
            .subscribe({
                next: (saved) => {
                    // Trust the response rather than the optimistic value.
                    this.preferences.set(saved);
                    // Push it to the tracker too: measurement must stop or start now, not on
                    // the next page load.
                    this.consent.set(saved.analyticsConsent ?? null);
                    this.messages.success(
                        this.translate.instant('profile.preferences.save.success'));
                },
                error: () => {
                    this.restoreToggle(input, previous);
                    this.messages.error(
                        this.translate.instant('profile.preferences.save.error'));
                },
            });
    }

    /**
     * Puts a checkbox back where the saved state says it should be.
     *
     * A switch rather than a ternary: with three preferences an `else` branch would silently
     * restore the wrong one — a failed analytics save would write the notifications value into the
     * analytics checkbox.
     */
    private restoreToggle(input: HTMLInputElement, saved: UserPreferences): void {
        switch (input.dataset['preference']) {
            case 'newsletter':
                // The switch reads "unsubscribe", so it shows the inverse of the stored value.
                input.checked = !saved.newsletterSubscribed;
                break;
            case 'notifications':
                input.checked = saved.notificationsEnabled;
                break;
            case 'analytics':
                input.checked = saved.analyticsConsent === true;
                break;
        }
    }
}

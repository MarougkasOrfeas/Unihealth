import {inject} from "@angular/core";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ConfirmationDialog} from "../../shared/components/confirm-dialog/confirm-dialog";
import {take} from "rxjs";
import {AuthService} from "./auth.service";
import {TranslateService} from "@ngx-translate/core";

export class AutoLogoutService {

    private dialog = inject(MatDialog);
    private translateService = inject(TranslateService);

    private readonly tokenWarningThresholdRatio = 0.8;
    private readonly authSyncChannelName = 'unihealth-auth-sync';
    private readonly authSyncStorageKey = 'unihealth-auth-sync-event';
    private readonly authSyncTabId =
        typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
            ? crypto.randomUUID()
            : `${Date.now()}-${Math.random().toString(36).slice(2)}`;

    private authSyncInitialized = false;
    private authSyncChannel: BroadcastChannel | null = null;
    private tokenWarningTimeoutId: ReturnType<typeof setTimeout> | null = null;
    private tokenWarningCountdownIntervalId: ReturnType<typeof setInterval> | null = null;
    private tokenWarningLogoutTimeoutId: ReturnType<typeof setTimeout> | null = null;
    private tokenWarningDialogRef: MatDialogRef<ConfirmationDialog, boolean> | null = null;

    constructor(private authService: AuthService) {
    }

    setupCrossTabAuthSync(): void {
        if (this.authSyncInitialized) {
            return;
        }
        this.authSyncInitialized = true;

        if (typeof BroadcastChannel !== 'undefined') {
            this.authSyncChannel = new BroadcastChannel(this.authSyncChannelName);
            this.authSyncChannel.onmessage = (event: MessageEvent<AuthSyncEvent>) => {
                this.handleAuthSyncEvent(event.data);
            };
        }

        window.addEventListener('storage', this.handleAuthSyncStorageEvent);
    }

    private readonly handleAuthSyncStorageEvent = (event: StorageEvent): void => {
        if (event.key !== this.authSyncStorageKey || !event.newValue) {
            return;
        }

        try {
            const payload = JSON.parse(event.newValue) as AuthSyncEvent;
            this.handleAuthSyncEvent(payload);
        } catch {
            // Ignore malformed cross-tab sync event payloads.
        }
    };

    private handleAuthSyncEvent(event: AuthSyncEvent): void {
        if (!event || event.sourceTabId === this.authSyncTabId) {
            return;
        }

        if (event.type === 'token_refreshed') {
            this.closeTokenWarningDialog();
            this.scheduleTokenExpirationWarning();
            return;
        }

        if (event.type === 'logout') {
            this.authService.performLogout();
        }
    }

    scheduleTokenExpirationWarning(): void {
        this.clearTokenExpirationWarningTimer();
        this.clearTokenWarningCountdown();
        this.clearTokenWarningLogoutTimer();

        const tokenPayload = this.getAccessTokenPayload();
        if (!tokenPayload?.iat || !tokenPayload?.exp) {
            return;
        }

        const issuedAtMs = tokenPayload.iat * 1000;
        const expiresAtMs = tokenPayload.exp * 1000;
        const tokenLifetimeMs = expiresAtMs - issuedAtMs;

        if (tokenLifetimeMs <= 0) {
            return;
        }

        const warningAtMs = issuedAtMs + tokenLifetimeMs * this.tokenWarningThresholdRatio;
        const warningDelayMs = Math.max(0, warningAtMs - Date.now());

        this.tokenWarningTimeoutId = setTimeout(() => {
            this.openTokenExpirationWarningDialog(expiresAtMs);
        }, warningDelayMs);
    }

    private getAccessTokenPayload(): { iat?: number; exp?: number } | null {
        const accessToken = this.authService.accessToken;
        if (!accessToken) {
            return null;
        }

        const tokenParts = accessToken.split('.');
        if (tokenParts.length < 2) {
            return null;
        }

        try {
            const payload = tokenParts[1]
                .replace(/-/g, '+')
                .replace(/_/g, '/');
            const padding = '='.repeat((4 - (payload.length % 4)) % 4);
            return JSON.parse(atob(payload + padding));
        } catch {
            return null;
        }
    }

    clearTokenExpirationWarningTimer(): void {
        if (!this.tokenWarningTimeoutId) {
            return;
        }

        clearTimeout(this.tokenWarningTimeoutId);
        this.tokenWarningTimeoutId = null;
    }

    private openTokenExpirationWarningDialog(expiresAtMs: number): void {
        if (!this.authService.authenticated || this.tokenWarningDialogRef) {
            return;
        }

        const initialSecondsRemaining = this.getSecondsRemaining(expiresAtMs);
        if (initialSecondsRemaining <= 0) {
            this.authService.logout();
            return;
        }

        this.startTokenWarningCountdown(expiresAtMs);
        this.startTokenWarningLogoutTimer(expiresAtMs);

        this.tokenWarningDialogRef = this.dialog.open(ConfirmationDialog, {
            disableClose: false,
            panelClass: 'confirm-exit-dialog',
            data: {
                title: 'auto.logout.dialog.title',
                message: this.buildTokenExpiryWarningMessage(initialSecondsRemaining),
                confirmText: 'auto.logout.dialog.confirm',
                cancelText: 'global.cancel',
            },
        });

        this.tokenWarningDialogRef.afterClosed().pipe(take(1)).subscribe((confirmed) => {
            this.clearTokenWarningCountdown();
            this.tokenWarningDialogRef = null;

            if (confirmed) {
                this.authService.refresh(true);
            }
        });
    }

    private startTokenWarningCountdown(expiresAtMs: number): void {
        this.clearTokenWarningCountdown();

        this.tokenWarningCountdownIntervalId = setInterval(() => {
            if (!this.tokenWarningDialogRef) {
                return;
            }

            const secondsRemaining = this.getSecondsRemaining(expiresAtMs);
            this.tokenWarningDialogRef.componentInstance.data.message =
                this.buildTokenExpiryWarningMessage(secondsRemaining);
        }, 1000);
    }

    private getSecondsRemaining(expiresAtMs: number): number {
        return Math.max(0, Math.ceil((expiresAtMs - Date.now()) / 1000));
    }

    private startTokenWarningLogoutTimer(expiresAtMs: number): void {
        this.clearTokenWarningLogoutTimer();

        const millisecondsUntilExpiration = Math.max(0, expiresAtMs - Date.now());
        this.tokenWarningLogoutTimeoutId = setTimeout(() => {
            this.authService.logout();
        }, millisecondsUntilExpiration);
    }

    private buildTokenExpiryWarningMessage(secondsRemaining: number): string {
        return this.translateService.instant('auto.logout.dialog.message').replace('${timer}', this.formatSecondsAsMinutes(secondsRemaining));
    }

    private formatSecondsAsMinutes(totalSeconds: number): string {
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }

    clearTokenWarningCountdown(): void {
        if (!this.tokenWarningCountdownIntervalId) {
            return;
        }

        clearInterval(this.tokenWarningCountdownIntervalId);
        this.tokenWarningCountdownIntervalId = null;
    }

    clearTokenWarningLogoutTimer(): void {
        if (!this.tokenWarningLogoutTimeoutId) {
            return;
        }

        clearTimeout(this.tokenWarningLogoutTimeoutId);
        this.tokenWarningLogoutTimeoutId = null;
    }

    closeTokenWarningDialog(): void {
        if (!this.tokenWarningDialogRef) {
            return;
        }

        this.tokenWarningDialogRef.close(false);
        this.tokenWarningDialogRef = null;
    }

    publishAuthSyncEvent(type: AuthSyncEventType): void {
        const payload: AuthSyncEvent = {
            type,
            sourceTabId: this.authSyncTabId,
            timestamp: Date.now(),
        };

        if (this.authSyncChannel) {
            this.authSyncChannel.postMessage(payload);
        }

        localStorage.setItem(this.authSyncStorageKey, JSON.stringify(payload));
        localStorage.removeItem(this.authSyncStorageKey);
    }
}

type AuthSyncEventType = 'token_refreshed' | 'logout';

interface AuthSyncEvent {
    type: AuthSyncEventType;
    sourceTabId: string;
    timestamp: number;
}

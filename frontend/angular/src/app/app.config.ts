import {
    ApplicationConfig, inject, LOCALE_ID, provideAppInitializer, provideBrowserGlobalErrorListeners,
    provideZoneChangeDetection
} from '@angular/core';
import {provideRouter} from '@angular/router';

import {routes} from './app.routes';
import {OAuthStorage, provideOAuthClient} from "angular-oauth2-oidc";
import {HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {MAT_FORM_FIELD_DEFAULT_OPTIONS} from "@angular/material/form-field";
import {TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {AuthService} from "./core/auth/auth.service";
import {UNIHEALTH_CONSTANTS} from "./shared/constants/unihealth.constants";
import {CheckAuthorizedInterceptor} from "./shared/interceptors/check-authorized.interceptor";
import {createTranslateLoader} from "./shared/services/translation.service";
import {Paginator} from "./shared/components/i18n/paginator";
import {MatPaginatorIntl} from "@angular/material/paginator";

export const appConfig: ApplicationConfig = {
    providers: [
        {provide: LOCALE_ID, useValue: 'el-GR'}, {provide: MatPaginatorIntl, useClass: Paginator},
        provideBrowserGlobalErrorListeners(),
        provideZoneChangeDetection({eventCoalescing: true}),
        provideRouter(routes),
        provideHttpClient(withInterceptorsFromDi()),
        provideAppInitializer(() => {
            const authService = inject(AuthService);
            return authService.init();
        }),
        provideOAuthClient({
            resourceServer: {
                allowedUrls: [UNIHEALTH_CONSTANTS.UNIHEALTH_API],
                sendAccessToken: true
            }
        }),
        {provide: OAuthStorage, useValue: localStorage},
        {provide: HTTP_INTERCEPTORS, useClass: CheckAuthorizedInterceptor, multi: true},
        {
            provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
            useValue: {appearance: 'outline'}
        },
        ...TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: createTranslateLoader,
                deps: [HttpClient],
            },
        }).providers!,
    ]
};

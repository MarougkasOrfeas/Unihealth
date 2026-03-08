import { HttpClient } from '@angular/common/http';
import { TranslateLoader } from '@ngx-translate/core';
import { map, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import {UNIHEALTH_CONSTANTS} from "../constants/unihealth.constants";

export interface Language {
  id: string;
  locale: string;
  key: string;
  label: string;
}

/**
 * Custom ngx-translate loader that fetches translations from the backend
 * via GET /api/translation/{locale}.
 *
 * The backend returns a flat Map&lt;String, String&gt; which is exactly what
 * ngx-translate expects.
 */
@Injectable({providedIn: 'root'})
export class TranslationsService implements TranslateLoader {
  constructor(private readonly http: HttpClient) {}

  getTranslation(lang: string): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(
      `${UNIHEALTH_CONSTANTS.UNIHEALTH_API}${UNIHEALTH_CONSTANTS.TRANSLATION_API.BASE}/${lang}`,
    );
  }

  getLanguages(): Observable<Language[]> {
    return this.http.get<Language[]>(`${UNIHEALTH_CONSTANTS.UNIHEALTH_API}${UNIHEALTH_CONSTANTS.TRANSLATION_API.BASE}/languages`).pipe(
      map((languages) =>
        languages.map((language) => ({
          ...language,
          key: language.id,
          label: `global.language.${language.locale}`,
        }))
      )
    );
  }
}

/**
 * Factory function for providing the custom loader via DI.
 */
export function createTranslateLoader(http: HttpClient): TranslationsService {
  return new TranslationsService(http);
}

import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';

/**
 * Backend `frontendMessageLabel` values mapped to lexicon keys. Pages own their maps,
 * this util only applies whatever it is handed.
 */
export type BackendErrorLabels = Readonly<Record<string, string>>;

/**
 * Extracts a readable, translated error message from an HTTP error response.
 *
 * Resolution order:
 *  1. Network error (status 0) → 'global.back.end.unavailable'
 *  2. String body               → translated via `labels` when known, else body text (trimmed)
 *  3. `error.message` string    → message text (trimmed)
 *  4. `error.label` string      → translated via `labels` when known, else the label as key
 *  5. Fallback                  → 'global.unexpected.error'
 *
 * The backend answers business errors with the bare label as the response body, so a
 * page that wants those translated passes its label map. Unknown strings pass through
 * because some flows (imports) reply with readable text.
 */
export function errorMessageFromHttp(
  err: HttpErrorResponse,
  translate: TranslateService,
  labels: BackendErrorLabels = {},
): string {
  if (err.status === 0) {
    return translate.instant('global.back.end.unavailable');
  }
  if (typeof err.error === 'string' && err.error.trim()) {
    const body = err.error.trim();
    return labels[body] ? translate.instant(labels[body]) : body;
  }
  if (typeof err.error?.message === 'string' && err.error.message.trim()) {
    return err.error.message.trim();
  }
  if (typeof err.error?.label === 'string' && err.error.label.trim()) {
    const label = err.error.label.trim();
    return translate.instant(labels[label] ?? label);
  }
  return translate.instant('global.unexpected.error');
}

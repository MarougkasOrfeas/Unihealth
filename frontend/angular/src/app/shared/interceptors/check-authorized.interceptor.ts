import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { AuthService } from "../../core/auth/auth.service";
import { catchError, Observable, tap, throwError } from "rxjs";

/**
 * Requests that must not drive the session.
 *
 * `/keycloak/` is the identity provider itself — refreshing a token off its own response would
 * recurse.
 *
 * `/usage/` is the fire-and-forget usage measurement endpoint. It is called far more often than
 * anything else and is entirely non-essential, so it must neither extend the session on success
 * (a background flush is not evidence the user is active) nor end it on failure (a transient 401
 * on a beacon-style write must never log anyone out).
 */
const SESSION_NEUTRAL_PATHS = ['/keycloak/', '/usage/'];

const isSessionNeutral = (url: string): boolean =>
  SESSION_NEUTRAL_PATHS.some((path) => url.includes(path));

@Injectable()
export class CheckAuthorizedInterceptor implements HttpInterceptor {

  private authService = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      tap(() => {
        if (!isSessionNeutral(req.url)) {
          this.authService.refresh();
        }
      }),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse
          && error.status === 401
          && !isSessionNeutral(req.url)) {

          this.authService.logout();
        }

        return throwError(() => error);
      })
    );
  }
}

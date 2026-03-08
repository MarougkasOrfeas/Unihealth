import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { AuthService } from "../../core/auth/auth.service";
import { catchError, Observable, tap, throwError } from "rxjs";
import { BeforeUnloadService } from "../services/beforeunload.service";

@Injectable()
export class CheckAuthorizedInterceptor implements HttpInterceptor {

  private authService = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      tap(() => {
        if (!req.url.includes('/keycloak/')) {
          this.authService.refresh();
        }
      }),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse
          && error.status === 401
          && !req.url.includes('/keycloak/')) {

          this.authService.logout();
        }

        return throwError(() => error);
      })
    );
  }
}

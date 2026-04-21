import { Injectable, inject } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'An unexpected error occurred';

        if (error.error instanceof ErrorEvent) {
          errorMessage = `Error: ${error.error.message}`;
        } else {
          switch (error.status) {
            case 400:
              errorMessage = error.error?.message || 'Bad request';
              break;
            case 401:
              errorMessage = 'Please log in to continue';
              break;
            case 403:
              errorMessage = 'You do not have permission to perform this action';
              break;
            case 404:
              errorMessage = 'Resource not found';
              break;
            case 409:
              errorMessage = error.error?.message || 'Conflict occurred';
              break;
            case 500:
              errorMessage = 'Server error. Please try again later';
              break;
            default:
              errorMessage = error.error?.message || `Error: ${error.message}`;
          }
        }

        console.error('[HTTP Error]', errorMessage, error);
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}

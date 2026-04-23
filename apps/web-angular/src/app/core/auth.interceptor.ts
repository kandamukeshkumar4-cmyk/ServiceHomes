import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, switchMap, take, catchError, of } from 'rxjs';
import { AppAuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AppAuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (req.url.startsWith('http') || this.isAnonymousGet(req)) {
      return next.handle(req);
    }

    return this.auth.getAccessToken().pipe(
      take(1),
      catchError(() => of(undefined)),
      switchMap(token => {
        if (token) {
          req = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` }
          });
        }
        return next.handle(req);
      })
    );
  }

  private isAnonymousGet(req: HttpRequest<unknown>): boolean {
    if (req.method !== 'GET') {
      return false;
    }

    const path = req.url.split('?')[0];
    return path === '/api/listings/search'
      || path === '/api/listings/categories'
      || path === '/api/listings/amenities'
      || /^\/api\/hosts\/[^/]+$/.test(path)
      || /^\/api\/listings\/[^/]+$/.test(path)
      || /^\/api\/listings\/[^/]+\/availability$/.test(path)
      || /^\/api\/listings\/[^/]+\/photos$/.test(path)
      || /^\/api\/listings\/[^/]+\/reviews$/.test(path);
  }
}

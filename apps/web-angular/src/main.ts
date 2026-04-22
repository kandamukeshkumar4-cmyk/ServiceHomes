import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { provideAuth0 } from '@auth0/auth0-angular';
import { routes } from './app/app.routes';
import { AuthInterceptor } from './app/core/auth.interceptor';
import { ErrorInterceptor } from './app/core/error.interceptor';
import { environment } from './environments/environment';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    ...(environment.auth.enabled ? [
      provideAuth0({
        domain: environment.auth.domain,
        clientId: environment.auth.clientId,
        authorizationParams: {
          redirect_uri: window.location.origin,
          audience: environment.auth.audience
        },
        useRefreshTokens: environment.auth.useRefreshTokens,
        cacheLocation: environment.auth.cacheLocation
      })
    ] : [])
  ]
}).catch(err => console.error(err));

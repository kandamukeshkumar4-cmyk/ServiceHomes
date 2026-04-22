import { Injectable, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { switchMap, catchError, shareReplay, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface Me {
  id: string;
  email: string;
  emailVerified: boolean;
  roles: string[];
  profile: {
    firstName: string;
    lastName: string;
    displayName: string;
    bio: string;
    avatarUrl: string;
    phoneNumber: string;
  } | null;
}

@Injectable({ providedIn: 'root' })
export class AppAuthService {
  private auth = inject(AuthService, { optional: true });
  private http = inject(HttpClient);
  private readonly localMode = !environment.auth.enabled || !this.auth;

  private readonly localMe$ = this.http.get<Me>('/api/me').pipe(
    catchError(() => of(null)),
    shareReplay(1)
  );

  private me$ = this.localMode
    ? this.localMe$
    : this.auth!.isAuthenticated$.pipe(
        switchMap(isAuth => {
          if (!isAuth) return of(null);
          return this.http.get<Me>('/api/me').pipe(
            catchError(() => of(null))
          );
        }),
        shareReplay(1)
      );

  private readonly authUser$ = this.localMode
    ? this.me$.pipe(
        map(me => me ? {
          name: me.profile?.displayName || me.email,
          email: me.email
        } : null)
      )
    : this.auth!.user$;

  get isAuthenticated$() {
    return this.localMode ? of(true) : this.auth!.isAuthenticated$;
  }

  get isLoading$() {
    return this.localMode ? of(false) : this.auth!.isLoading$;
  }

  get user$() {
    return this.authUser$;
  }

  get me(): Observable<Me | null> {
    return this.me$;
  }

  get usesAuth0(): boolean {
    return !this.localMode;
  }

  get isLocalMode(): boolean {
    return this.localMode;
  }

  login() {
    if (!this.localMode) {
      this.auth!.loginWithRedirect();
    }
  }

  logout() {
    if (!this.localMode) {
      this.auth!.logout({ logoutParams: { returnTo: window.location.origin } });
    }
  }

  getAccessToken(): Observable<string | undefined> {
    if (this.localMode) {
      return of(undefined);
    }

    return this.auth!.getAccessTokenSilently().pipe(
      catchError(() => of(undefined))
    );
  }
}

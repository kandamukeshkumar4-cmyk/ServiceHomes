import { Injectable, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { switchMap, catchError, shareReplay } from 'rxjs/operators';

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
  private auth = inject(AuthService);
  private http = inject(HttpClient);

  private me$ = this.auth.isAuthenticated$.pipe(
    switchMap(isAuth => {
      if (!isAuth) return of(null);
      return this.http.get<Me>('/api/me').pipe(
        catchError(() => of(null))
      );
    }),
    shareReplay(1)
  );

  get isAuthenticated$() {
    return this.auth.isAuthenticated$;
  }

  get user$() {
    return this.auth.user$;
  }

  get me(): Observable<Me | null> {
    return this.me$;
  }

  login() {
    this.auth.loginWithRedirect();
  }

  logout() {
    this.auth.logout({ logoutParams: { returnTo: window.location.origin } });
  }

  getAccessToken(): Observable<string | undefined> {
    return this.auth.getAccessTokenSilently();
  }
}

import { Injectable, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { HttpClient } from '@angular/common/http';
import { Observable, of, BehaviorSubject, combineLatest } from 'rxjs';
import { switchMap, catchError, shareReplay, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ListingCardDto } from '../listings/listing.model';

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
    location: string;
    languages: string[];
    createdAt: string;
  } | null;
}

export interface UpdateProfilePayload {
  displayName?: string;
  bio?: string;
  avatarUrl?: string;
  phoneNumber?: string;
  location?: string;
  languages?: string[];
}

export interface AvatarUploadTarget {
  uploadUrl: string;
  publicUrl: string;
}

export interface HostProfile {
  hostId: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  location: string | null;
  languages: string[];
  memberSince: string;
  responseRate: number | null;
  listingsCount: number;
  listings: ListingCardDto[];
}

@Injectable({ providedIn: 'root' })
export class AppAuthService {
  private auth = inject(AuthService, { optional: true });
  private http = inject(HttpClient);
  private readonly localMode = !environment.auth.enabled || !this.auth;
  private readonly meRefresh$ = new BehaviorSubject(0);

  private readonly localMe$ = this.meRefresh$.pipe(
    switchMap(() => this.http.get<Me>('/api/me').pipe(
      catchError(() => of(null))
    )),
    shareReplay(1)
  );

  private me$ = this.localMode
    ? this.localMe$
    : combineLatest([this.auth!.isAuthenticated$, this.meRefresh$]).pipe(
        switchMap(([isAuth]) => {
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

  refreshMe() {
    this.meRefresh$.next(this.meRefresh$.value + 1);
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

  updateProfile(payload: UpdateProfilePayload): Observable<Me> {
    return this.http.patch<Me>('/api/me/profile', payload).pipe(
      tap(() => this.refreshMe())
    );
  }

  becomeHost(): Observable<Me> {
    return this.http.post<Me>('/api/me/become-host', {}).pipe(
      tap(() => this.refreshMe())
    );
  }

  createAvatarUploadTarget(fileName: string, contentType: string): Observable<AvatarUploadTarget> {
    return this.http.post<AvatarUploadTarget>('/api/me/profile/avatar-upload-url', { fileName, contentType });
  }

  uploadAvatar(file: File): Observable<string> {
    return this.createAvatarUploadTarget(file.name, file.type).pipe(
      switchMap(target =>
        this.http.put(target.uploadUrl, file, {
          headers: { 'Content-Type': file.type },
          responseType: 'text'
        }).pipe(
          map(() => target.publicUrl)
        )
      )
    );
  }

  getHostProfile(hostId: string): Observable<HostProfile> {
    return this.http.get<HostProfile>(`/api/hosts/${hostId}`);
  }
}

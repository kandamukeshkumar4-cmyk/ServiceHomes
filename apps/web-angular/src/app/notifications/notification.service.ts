import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, catchError, first, map, of, switchMap, tap, timer } from 'rxjs';
import { AppAuthService } from '../core/auth.service';
import { AppNotification, NotificationPage, NotificationPreference, NotificationType, NotificationChannel } from './notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AppAuthService);
  private readonly notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  private readonly unreadCountSubject = new BehaviorSubject(0);
  private socket?: WebSocket;
  private reconnectSubscription?: Subscription;
  private reconnectAttempts = 0;

  readonly notifications$ = this.notificationsSubject.asObservable();
  readonly unreadCount$ = this.unreadCountSubject.asObservable();

  constructor() {
    this.auth.isAuthenticated$.subscribe((isAuthenticated) => {
      if (isAuthenticated) {
        this.refresh();
        this.connectWebSocket();
      } else {
        this.disconnect();
      }
    });
  }

  connectWebSocket(): void {
    if (this.socket && (this.socket.readyState === WebSocket.CONNECTING || this.socket.readyState === WebSocket.OPEN)) {
      return;
    }

    this.resolveWebSocketToken().subscribe((token) => {
      if (!token) {
        return;
      }

      this.socket = new WebSocket(`${this.webSocketBaseUrl()}/ws/notifications?token=${encodeURIComponent(token)}`);
      this.socket.onopen = () => {
        this.reconnectAttempts = 0;
        this.reconnectSubscription?.unsubscribe();
      };
      this.socket.onmessage = (event) => this.receive(JSON.parse(event.data) as AppNotification);
      this.socket.onclose = () => this.scheduleReconnect();
      this.socket.onerror = () => this.socket?.close();
    });
  }

  getNotifications(page = 0, size = 20): Observable<NotificationPage> {
    return this.http.get<NotificationPage>(`/api/notifications?page=${page}&size=${size}`).pipe(
      tap((result) => this.notificationsSubject.next(result.content))
    );
  }

  getUnreadCount(): Observable<number> {
    return this.http.get<{ count: number }>('/api/notifications/unread-count').pipe(
      map((response) => response.count),
      tap((count) => this.unreadCountSubject.next(count))
    );
  }

  markAsRead(id: string): Observable<AppNotification> {
    return this.http.post<AppNotification>(`/api/notifications/${id}/read`, {}).pipe(
      tap((updated) => this.replaceNotification(updated))
    );
  }

  markAllAsRead(): Observable<{ updated: number }> {
    return this.http.post<{ updated: number }>('/api/notifications/read-all', {}).pipe(
      tap(() => {
        this.notificationsSubject.next(this.notificationsSubject.value.map((notification) => ({ ...notification, read: true })));
        this.unreadCountSubject.next(0);
      })
    );
  }

  dismiss(id: string): Observable<void> {
    return this.http.delete<void>(`/api/notifications/${id}`).pipe(
      tap(() => {
        const removed = this.notificationsSubject.value.find((notification) => notification.id === id);
        this.notificationsSubject.next(this.notificationsSubject.value.filter((notification) => notification.id !== id));
        if (removed && !removed.read) {
          this.unreadCountSubject.next(Math.max(0, this.unreadCountSubject.value - 1));
        }
      })
    );
  }

  getPreferences(): Observable<NotificationPreference[]> {
    return this.http.get<NotificationPreference[]>('/api/notifications/preferences');
  }

  updatePreference(type: NotificationType, channel: NotificationChannel, enabled: boolean): Observable<NotificationPreference> {
    return this.http.put<NotificationPreference>('/api/notifications/preferences', { type, channel, enabled });
  }

  refresh(): void {
    this.getNotifications().pipe(catchError(() => of(null))).subscribe();
    this.getUnreadCount().pipe(catchError(() => of(0))).subscribe();
  }

  private receive(notification: AppNotification): void {
    const current = this.notificationsSubject.value.filter((item) => item.id !== notification.id);
    this.notificationsSubject.next([notification, ...current].slice(0, 50));
    if (!notification.read) {
      this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
    }
  }

  private replaceNotification(updated: AppNotification): void {
    const previous = this.notificationsSubject.value.find((notification) => notification.id === updated.id);
    this.notificationsSubject.next(this.notificationsSubject.value.map((notification) => notification.id === updated.id ? updated : notification));
    if (previous && !previous.read && updated.read) {
      this.unreadCountSubject.next(Math.max(0, this.unreadCountSubject.value - 1));
    }
  }

  private resolveWebSocketToken(): Observable<string | undefined> {
    return this.auth.getAccessToken().pipe(
      first(),
      switchMap((token) => {
        if (token) {
          return of(token);
        }
        return this.auth.me.pipe(first(), map((me) => me?.id));
      })
    );
  }

  private scheduleReconnect(): void {
    this.reconnectSubscription?.unsubscribe();
    const delay = Math.min(30000, 1000 * Math.pow(2, this.reconnectAttempts++));
    this.reconnectSubscription = timer(delay).subscribe(() => this.connectWebSocket());
  }

  private disconnect(): void {
    this.reconnectSubscription?.unsubscribe();
    this.socket?.close();
    this.socket = undefined;
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
  }

  private webSocketBaseUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}`;
  }
}

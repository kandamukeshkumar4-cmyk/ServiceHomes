import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, merge, of, shareReplay, switchMap, timer } from 'rxjs';
import { AppAuthService } from './auth.service';
import { MessagingService } from '../messaging/messaging.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly auth = inject(AppAuthService);
  private readonly messagingService = inject(MessagingService);
  private readonly manualRefresh$ = new EventTarget();

  readonly unreadMessagesCount$ = this.auth.isAuthenticated$.pipe(
    switchMap((isAuthenticated) => {
      if (!isAuthenticated) {
        return of(0);
      }

      return merge(
        of(null),
        timer(30000, 30000),
        fromEvent(this.manualRefresh$, 'refresh')
      ).pipe(
        switchMap(() => this.messagingService.getInbox().pipe(
          map((threads) => threads.reduce((total, thread) => total + thread.unreadCount, 0)),
          catchError(() => of(0))
        ))
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  refreshUnreadMessages(): void {
    this.manualRefresh$.dispatchEvent(new Event('refresh'));
  }
}

function fromEvent(target: EventTarget, name: string) {
  return new Observable<Event>((subscriber) => {
    const handler = (event: Event) => subscriber.next(event);
    target.addEventListener(name, handler);
    return () => target.removeEventListener(name, handler);
  });
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MessagingInboxThread, MessagingThread, SendMessagePayload } from './messaging.models';

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly http = inject(HttpClient);

  getInbox(): Observable<MessagingInboxThread[]> {
    return this.http.get<MessagingInboxThread[]>('/api/inbox');
  }

  getThread(reservationId: string): Observable<MessagingThread> {
    return this.http.get<MessagingThread>(`/api/reservations/${reservationId}/messages`);
  }

  sendMessage(reservationId: string, payload: SendMessagePayload): Observable<MessagingThread> {
    return this.http.post<MessagingThread>(`/api/reservations/${reservationId}/messages`, payload);
  }
}

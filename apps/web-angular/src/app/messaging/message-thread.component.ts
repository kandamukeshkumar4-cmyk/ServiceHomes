import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, switchMap, timer } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, tap } from 'rxjs/operators';
import { MessagingMessage, MessagingThread, PendingMessagingMessage } from './messaging.models';
import { MessagingService } from './messaging.service';

type ThreadMessageView = MessagingMessage | PendingMessagingMessage;

@Component({
  selector: 'app-message-thread',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './message-thread.component.html',
  styles: []
})
export class MessageThreadComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly messagingService = inject(MessagingService);

  readonly skeletonMessages = Array.from({ length: 4 }, (_, index) => index);
  readonly maxMessageLength = 5000;

  reservationId = '';
  draft = '';
  loading = true;
  refreshing = false;
  sending = false;
  loadError: string | null = null;
  sendError: string | null = null;
  thread: MessagingThread | null = null;
  pendingMessages: PendingMessagingMessage[] = [];

  ngOnInit(): void {
    this.route.paramMap.pipe(
      map((params) => params.get('reservationId') ?? params.get('id') ?? ''),
      tap((reservationId) => {
        if (!reservationId) {
          this.loading = false;
          this.loadError = 'Reservation not found.';
        }
      }),
      filter((reservationId): reservationId is string => reservationId.length > 0),
      distinctUntilChanged(),
      tap((reservationId) => {
        this.reservationId = reservationId;
        this.thread = null;
        this.pendingMessages = [];
        this.loading = true;
        this.refreshing = false;
        this.loadError = null;
        this.sendError = null;
      }),
      switchMap((reservationId) =>
        timer(0, 5000).pipe(
          tap((tick) => {
            this.refreshing = tick > 0 && !this.loading;
          }),
          switchMap(() =>
            this.messagingService.getThread(reservationId).pipe(
              tap((thread) => {
                this.thread = thread;
                this.loading = false;
                this.refreshing = false;
                this.loadError = null;
              }),
              catchError((error: unknown) => {
                this.loading = false;
                this.refreshing = false;
                this.loadError = this.describeError(error, 'Unable to load this conversation.');
                return EMPTY;
              })
            )
          )
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  get displayedMessages(): ThreadMessageView[] {
    const persistedMessages = this.thread?.messages ?? [];
    if (this.pendingMessages.length === 0) {
      return persistedMessages;
    }

    return [...persistedMessages, ...this.pendingMessages].sort(
      (left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime()
    );
  }

  get canSend(): boolean {
    const trimmed = this.draft.trim();
    return !this.sending && trimmed.length > 0 && trimmed.length <= this.maxMessageLength;
  }

  get remainingCharacters(): number {
    return this.maxMessageLength - this.draft.length;
  }

  sendMessage(): void {
    const content = this.draft.trim();
    if (!this.reservationId || !content || this.sending) {
      return;
    }

    if (content.length > this.maxMessageLength) {
      this.sendError = `Messages must be ${this.maxMessageLength} characters or fewer.`;
      return;
    }

    const pendingId = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? `pending-${crypto.randomUUID()}`
      : `pending-${Date.now()}`;

    const pendingMessage: PendingMessagingMessage = {
      id: pendingId,
      senderId: 'pending',
      senderDisplayName: 'You',
      senderAvatarUrl: null,
      content,
      createdAt: new Date().toISOString(),
      readAt: null,
      mine: true,
      pending: true
    };

    this.pendingMessages = [...this.pendingMessages, pendingMessage];
    this.draft = '';
    this.sending = true;
    this.sendError = null;

    this.messagingService.sendMessage(this.reservationId, { content }).subscribe({
      next: (thread) => {
        this.thread = thread;
        this.pendingMessages = this.pendingMessages.filter((message) => message.id !== pendingId);
        this.sending = false;
      },
      error: (error: unknown) => {
        this.pendingMessages = this.pendingMessages.filter((message) => message.id !== pendingId);
        this.draft = content;
        this.sendError = this.describeError(error, 'Unable to send your message right now.');
        this.sending = false;
      }
    });
  }

  trackByMessage(index: number, message: ThreadMessageView): string {
    return message.id;
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((segment) => segment[0]?.toUpperCase() ?? '')
      .join('') || '?';
  }

  isPending(message: ThreadMessageView): message is PendingMessagingMessage {
    return 'pending' in message && message.pending;
  }

  private describeError(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string' && error.error.trim().length > 0) {
        return error.error;
      }

      const message = error.error?.message;
      if (typeof message === 'string' && message.trim().length > 0) {
        return message;
      }
    }

    return fallback;
  }
}

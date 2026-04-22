import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MessagingInboxThread } from './messaging.models';
import { MessagingService } from './messaging.service';

@Component({
  selector: 'app-inbox',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './inbox.component.html',
  styles: []
})
export class InboxComponent implements OnInit {
  private readonly messagingService = inject(MessagingService);

  readonly skeletonRows = Array.from({ length: 4 }, (_, index) => index);

  loading = true;
  errorMessage: string | null = null;
  threads: MessagingInboxThread[] = [];

  ngOnInit(): void {
    this.loadInbox();
  }

  loadInbox(): void {
    this.loading = true;
    this.errorMessage = null;

    this.messagingService.getInbox().subscribe({
      next: (threads) => {
        this.threads = threads;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error, 'Unable to load your inbox right now.');
        this.loading = false;
      }
    });
  }

  trackByThread(index: number, thread: MessagingInboxThread): string {
    return thread.threadId;
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((segment) => segment[0]?.toUpperCase() ?? '')
      .join('') || '?';
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

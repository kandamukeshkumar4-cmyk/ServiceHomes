import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from './notification.service';
import { AppNotification } from './notification.model';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-center.component.html',
  styleUrl: './notification-center.component.scss'
})
export class NotificationCenterComponent {
  private readonly router = inject(Router);
  readonly notificationService = inject(NotificationService);
  open = false;

  toggle(): void {
    this.open = !this.open;
    if (this.open) {
      this.notificationService.refresh();
    }
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe();
  }

  dismiss(event: Event, notification: AppNotification): void {
    event.stopPropagation();
    this.notificationService.dismiss(notification.id).subscribe();
  }

  openNotification(notification: AppNotification): void {
    const target = typeof notification.data?.['path'] === 'string'
      ? notification.data['path'] as string
      : typeof notification.data?.['actionUrl'] === 'string'
        ? notification.data['actionUrl'] as string
        : '/notifications';
    this.notificationService.markAsRead(notification.id).subscribe(() => {
      this.open = false;
      this.router.navigateByUrl(target);
    });
  }

  iconFor(type: string): string {
    switch (type) {
      case 'MESSAGE_RECEIVED':
        return 'pi pi-comments';
      case 'RESERVATION_CREATED':
      case 'RESERVATION_CONFIRMED':
        return 'pi pi-calendar-check';
      case 'REVIEW_RECEIVED':
        return 'pi pi-star';
      case 'LISTING_PUBLISHED':
        return 'pi pi-home';
      default:
        return 'pi pi-bell';
    }
  }
}

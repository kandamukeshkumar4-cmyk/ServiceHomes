import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AppAuthService } from '../core/auth.service';
import { NotificationService } from '../core/notification.service';
import { NotificationCenterComponent } from '../notifications/notification-center.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, ButtonModule, NotificationCenterComponent],
  template: `
    <nav class="navbar flex justify-content-between align-items-center px-4 py-3 surface-0 shadow-1">
      <a routerLink="/home" class="text-2xl font-bold text-primary no-underline">ServiceHomes</a>
      <div class="flex align-items-center gap-3">
        <a routerLink="/home" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Home</a>
        <ng-container *ngIf="auth.isLoading$ | async; else authReady">
          <span class="text-600 text-sm">Loading account...</span>
        </ng-container>
        <ng-template #authReady>
          <ng-container *ngIf="auth.isAuthenticated$ | async; else loggedOut">
            <a routerLink="/saved" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Saved</a>
            <a routerLink="/inbox" routerLinkActive="text-primary font-bold" class="text-700 no-underline navbar-link-with-badge">
              <span>Messages</span>
              <span *ngIf="(notifications.unreadMessagesCount$ | async) as unreadCount" class="navbar-badge" [class.navbar-badge--hidden]="unreadCount < 1">
                {{ unreadCount }}
              </span>
            </a>
            <app-notification-center></app-notification-center>
            <a routerLink="/trips" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Trips</a>
            <a routerLink="/host" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Host</a>
            <a routerLink="/account" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Account</a>
            <span *ngIf="auth.isLocalMode" class="px-2 py-1 surface-100 border-round text-600 text-sm">Local Auth</span>
            <button *ngIf="auth.usesAuth0" pButton class="p-button-text p-button-sm" (click)="auth.logout()">Log out</button>
          </ng-container>
        </ng-template>
        <ng-template #loggedOut>
          <button pButton class="p-button-primary p-button-sm" (click)="auth.login()">Log in</button>
        </ng-template>
      </div>
    </nav>
  `,
  styles: [`
    .navbar { position: sticky; top: 0; z-index: 1000; }
    .navbar-link-with-badge {
      align-items: center;
      display: inline-flex;
      gap: 0.45rem;
    }
    .navbar-badge {
      align-items: center;
      background: #dc2626;
      border-radius: 999px;
      color: #fff;
      display: inline-flex;
      font-size: 0.7rem;
      font-weight: 700;
      height: 1.15rem;
      justify-content: center;
      min-width: 1.15rem;
      padding: 0 0.3rem;
    }
    .navbar-badge--hidden {
      display: none;
    }
  `]
})
export class NavbarComponent {
  auth = inject(AppAuthService);
  notifications = inject(NotificationService);
}

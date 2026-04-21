import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppAuthService } from '../core/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar flex justify-content-between align-items-center px-4 py-3 surface-0 shadow-1">
      <a routerLink="/home" class="text-2xl font-bold text-primary no-underline">ServiceHomes</a>
      <div class="flex align-items-center gap-3">
        <a routerLink="/home" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Home</a>
        <ng-container *ngIf="auth.isAuthenticated$ | async; else loggedOut">
          <a routerLink="/bookings" routerLinkActive="text-primary font-bold" class="text-700 no-underline">My Bookings</a>
          <a routerLink="/host/accommodations" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Host</a>
          <a routerLink="/account" routerLinkActive="text-primary font-bold" class="text-700 no-underline">Account</a>
          <button pButton class="p-button-text p-button-sm" (click)="auth.logout()">Log out</button>
        </ng-container>
        <ng-template #loggedOut>
          <button pButton class="p-button-primary p-button-sm" (click)="auth.login()">Log in</button>
        </ng-template>
      </div>
    </nav>
  `,
  styles: [`
    .navbar { position: sticky; top: 0; z-index: 1000; }
  `]
})
export class NavbarComponent {
  auth = inject(AppAuthService);
}

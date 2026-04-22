import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppAuthService } from '../core/auth.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-4 max-w-30rem mx-auto">
      <h1 class="text-2xl font-bold mb-3">Account</h1>
      <div *ngIf="auth.isLoading$ | async; else loaded">
        <p>Loading account...</p>
      </div>
      <ng-template #loaded>
        <div *ngIf="auth.me | async as me; else signedOut">
          <div class="flex align-items-center gap-3 mb-3">
            <img *ngIf="me.profile?.avatarUrl" [src]="me.profile!.avatarUrl" class="w-4rem h-4rem border-circle" alt="avatar" />
            <div>
              <p class="text-xl font-bold m-0">{{ me.profile?.displayName || me.email }}</p>
              <p class="text-600 m-0">{{ me.email }}</p>
            </div>
          </div>
          <p *ngIf="auth.isLocalMode" class="surface-100 p-2 border-round text-600 text-sm">
            Local development mode is active. The frontend is using the backend's local auth bypass instead of Auth0.
          </p>
          <div class="surface-100 p-3 border-round-lg">
            <p><strong>Roles:</strong> {{ me.roles.join(', ') }}</p>
            <p><strong>Verified:</strong> {{ me.emailVerified ? 'Yes' : 'No' }}</p>
          </div>
        </div>
      </ng-template>
      <ng-template #signedOut>
        <div class="surface-100 p-3 border-round-lg">
          <p class="m-0 mb-3">Sign in with Auth0 to view and manage your account.</p>
          <button pButton class="p-button-primary" (click)="auth.login()">Log in</button>
        </div>
      </ng-template>
    </div>
  `,
  styles: []
})
export class AccountComponent {
  auth = inject(AppAuthService);
}

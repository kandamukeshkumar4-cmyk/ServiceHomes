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
      <div *ngIf="auth.me | async as me; else loading">
        <div class="flex align-items-center gap-3 mb-3">
          <img *ngIf="me.profile?.avatarUrl" [src]="me.profile!.avatarUrl" class="w-4rem h-4rem border-circle" alt="avatar" />
          <div>
            <p class="text-xl font-bold m-0">{{ me.profile?.displayName || me.email }}</p>
            <p class="text-600 m-0">{{ me.email }}</p>
          </div>
        </div>
        <div class="surface-100 p-3 border-round-lg">
          <p><strong>Roles:</strong> {{ me.roles.join(', ') }}</p>
          <p><strong>Verified:</strong> {{ me.emailVerified ? 'Yes' : 'No' }}</p>
        </div>
      </div>
      <ng-template #loading>
        <p>Loading profile...</p>
      </ng-template>
    </div>
  `,
  styles: []
})
export class AccountComponent {
  auth = inject(AppAuthService);
}

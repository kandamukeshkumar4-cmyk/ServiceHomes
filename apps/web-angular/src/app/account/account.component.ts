import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppAuthService, Me, UpdateProfilePayload } from '../core/auth.service';
import { RecentlyViewedCarouselComponent } from '../wishlists/recently-viewed-carousel.component';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RecentlyViewedCarouselComponent],
  template: `
    <div class="p-4 max-w-60rem mx-auto">
      <h1 class="text-2xl font-bold mb-3">Profile & hosting</h1>

      <div *ngIf="auth.isLoading$ | async; else loaded">
        <p>Loading account...</p>
      </div>

      <ng-template #loaded>
        <div *ngIf="me; else signedOut" class="grid gap-4">
          <div class="col-12 lg:col-4">
            <div class="surface-0 shadow-1 border-round-xl p-4">
              <div class="flex flex-column align-items-center text-center gap-3">
                <div class="w-8rem h-8rem border-circle overflow-hidden surface-200 flex align-items-center justify-content-center">
                  <img *ngIf="form.avatarUrl" [src]="form.avatarUrl" class="w-full h-full object-cover" alt="avatar" />
                  <span *ngIf="!form.avatarUrl" class="text-600 font-semibold text-sm">No avatar</span>
                </div>

                <div>
                  <p class="text-xl font-bold m-0">{{ form.displayName || me.email }}</p>
                  <p class="text-600 m-0">{{ me.email }}</p>
                </div>

                <input type="file" accept="image/*" (change)="onAvatarSelected($event)" #avatarInput hidden />
                <button type="button" class="p-button p-button-outlined w-full" (click)="avatarInput.click()" [disabled]="uploadingAvatar">
                  {{ uploadingAvatar ? 'Uploading avatar...' : 'Upload avatar' }}
                </button>

                <div class="surface-100 border-round-lg p-3 w-full text-left">
                  <p class="m-0"><strong>Roles:</strong> {{ roleLabel }}</p>
                  <p class="m-0 mt-2"><strong>Email verified:</strong> {{ me.emailVerified ? 'Yes' : 'No' }}</p>
                  <p class="m-0 mt-2" *ngIf="me.profile?.createdAt"><strong>Profile created:</strong> {{ me.profile!.createdAt | date:'mediumDate' }}</p>
                </div>

                <p *ngIf="auth.isLocalMode" class="surface-100 p-2 border-round text-600 text-sm m-0">
                  Local development mode is active. The frontend is using the backend's local auth bypass instead of Auth0.
                </p>

                <div class="w-full surface-100 border-round-lg p-3" *ngIf="isHost; else becomeHostBlock">
                  <p class="font-semibold mt-0 mb-2">Host account active</p>
                  <p class="text-600 text-sm mt-0 mb-3">Your account can publish listings and manage hosting tools.</p>
                  <a routerLink="/host/accommodations" class="p-button p-button-primary no-underline w-full text-center block">Open hosting dashboard</a>
                </div>

                <ng-template #becomeHostBlock>
                  <div class="w-full surface-100 border-round-lg p-3">
                    <p class="font-semibold mt-0 mb-2">Become a host</p>
                    <p class="text-600 text-sm mt-0 mb-3">Turn your traveler account into a hosting account so you can publish listings.</p>
                    <button
                      type="button"
                      class="p-button p-button-primary w-full"
                      [disabled]="!me.emailVerified || becomingHost"
                      (click)="showBecomeHostConfirm = true">
                      {{ becomingHost ? 'Updating role...' : 'Become a host' }}
                    </button>
                    <p *ngIf="!me.emailVerified" class="text-600 text-sm mt-2 mb-0">Verify your email before becoming a host.</p>
                  </div>
                </ng-template>
              </div>
            </div>
          </div>

          <div class="col-12 lg:col-8">
            <div class="surface-0 shadow-1 border-round-xl p-4">
              <div class="flex justify-content-between align-items-center mb-3">
                <h2 class="text-xl font-bold m-0">Edit profile</h2>
                <button type="button" class="p-button p-button-primary" (click)="saveProfile()" [disabled]="saving">
                  {{ saving ? 'Saving...' : 'Save profile' }}
                </button>
              </div>

              <div *ngIf="saveError" class="surface-100 border-round-lg p-3 text-red-600 mb-3">{{ saveError }}</div>
              <div *ngIf="saveSuccess" class="surface-100 border-round-lg p-3 text-green-700 mb-3">{{ saveSuccess }}</div>

              <div class="grid gap-3">
                <div class="col-12 md:col-6 flex flex-column gap-2">
                  <label for="displayName">Display name</label>
                  <input id="displayName" class="p-inputtext" [(ngModel)]="form.displayName" />
                </div>
                <div class="col-12 md:col-6 flex flex-column gap-2">
                  <label for="phone">Phone</label>
                  <input id="phone" class="p-inputtext" [(ngModel)]="form.phoneNumber" />
                </div>
                <div class="col-12 flex flex-column gap-2">
                  <label for="bio">Bio</label>
                  <textarea id="bio" class="p-inputtext" rows="4" [(ngModel)]="form.bio"></textarea>
                </div>
                <div class="col-12 md:col-6 flex flex-column gap-2">
                  <label for="location">Location</label>
                  <input id="location" class="p-inputtext" [(ngModel)]="form.location" placeholder="City, Country" />
                </div>
                <div class="col-12 md:col-6 flex flex-column gap-2">
                  <label for="languages">Languages</label>
                  <input id="languages" class="p-inputtext" [(ngModel)]="form.languagesText" placeholder="English, Spanish" />
                </div>
              </div>
            </div>
          </div>

          <div class="col-12">
            <app-recently-viewed-carousel></app-recently-viewed-carousel>
          </div>
        </div>
      </ng-template>

      <ng-template #signedOut>
        <div class="surface-100 p-3 border-round-lg">
          <p class="m-0 mb-3">Sign in with Auth0 to view and manage your account.</p>
          <button type="button" class="p-button p-button-primary" (click)="auth.login()">Log in</button>
        </div>
      </ng-template>
    </div>

    <div *ngIf="showBecomeHostConfirm" class="fixed inset-0 flex align-items-center justify-content-center" style="background: rgba(15, 23, 42, 0.55); z-index: 20;">
      <div class="surface-0 border-round-xl shadow-4 p-4 max-w-24rem w-full mx-3">
        <h3 class="text-xl font-bold mt-0 mb-2">Become a host?</h3>
        <p class="text-600 mt-0 mb-3">This enables hosting tools for your account. Your traveler access will still remain available.</p>
        <div *ngIf="becomeHostError" class="surface-100 border-round-lg p-2 text-red-600 mb-3">{{ becomeHostError }}</div>
        <div class="flex gap-2">
          <button type="button" class="p-button p-button-outlined flex-1" (click)="closeBecomeHostConfirm()" [disabled]="becomingHost">Cancel</button>
          <button type="button" class="p-button p-button-primary flex-1" (click)="confirmBecomeHost()" [disabled]="becomingHost">
            {{ becomingHost ? 'Confirming...' : 'Confirm' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AccountComponent implements OnInit {
  auth = inject(AppAuthService);
  me: Me | null = null;
  saving = false;
  uploadingAvatar = false;
  becomingHost = false;
  showBecomeHostConfirm = false;
  saveError = '';
  saveSuccess = '';
  becomeHostError = '';

  form = {
    displayName: '',
    bio: '',
    avatarUrl: '',
    phoneNumber: '',
    location: '',
    languagesText: ''
  };

  ngOnInit() {
    this.auth.me.subscribe(me => {
      this.me = me;
      if (me) {
        this.populateForm(me);
      }
    });
  }

  get isHost(): boolean {
    return !!this.me?.roles.includes('HOST');
  }

  get roleLabel(): string {
    return (this.me?.roles ?? [])
      .map(role => role === 'TRAVELER' ? 'Traveler' : role.charAt(0) + role.slice(1).toLowerCase())
      .join(', ');
  }

  saveProfile(successMessage = 'Profile saved.') {
    if (!this.me || this.saving) {
      return;
    }
    this.saving = true;
    this.saveError = '';
    this.saveSuccess = '';
    this.auth.updateProfile(this.buildPayload()).subscribe({
      next: me => {
        this.me = me;
        this.populateForm(me);
        this.saving = false;
        this.saveSuccess = successMessage;
      },
      error: error => {
        this.saveError = error?.error?.message || 'Unable to save your profile right now.';
        this.saving = false;
      }
    });
  }

  onAvatarSelected(event: Event) {
    const files = (event.target as HTMLInputElement).files;
    const file = files?.item(0);
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.saveError = 'Please choose an image file for your avatar.';
      return;
    }

    this.uploadingAvatar = true;
    this.saveError = '';
    this.saveSuccess = '';
    this.auth.uploadAvatar(file).subscribe({
      next: publicUrl => {
        this.form.avatarUrl = publicUrl;
        this.uploadingAvatar = false;
        this.saveProfile('Avatar uploaded and profile saved.');
      },
      error: error => {
        this.saveError = error?.error?.message || 'Unable to upload your avatar right now.';
        this.uploadingAvatar = false;
      }
    });
  }

  closeBecomeHostConfirm() {
    this.showBecomeHostConfirm = false;
    this.becomeHostError = '';
  }

  confirmBecomeHost() {
    if (!this.me || this.becomingHost) {
      return;
    }
    this.becomingHost = true;
    this.becomeHostError = '';
    this.auth.becomeHost().subscribe({
      next: me => {
        this.me = me;
        this.populateForm(me);
        this.becomingHost = false;
        this.showBecomeHostConfirm = false;
        this.saveSuccess = 'Your account can now host listings.';
      },
      error: error => {
        this.becomeHostError = error?.error?.message || 'Unable to enable hosting for this account.';
        this.becomingHost = false;
      }
    });
  }

  private buildPayload(): UpdateProfilePayload {
    return {
      displayName: this.form.displayName || undefined,
      bio: this.form.bio || undefined,
      avatarUrl: this.form.avatarUrl || undefined,
      phoneNumber: this.form.phoneNumber || undefined,
      location: this.form.location || undefined,
      languages: this.form.languagesText
        .split(',')
        .map(language => language.trim())
        .filter(Boolean)
    };
  }

  private populateForm(me: Me) {
    this.form = {
      displayName: me.profile?.displayName || '',
      bio: me.profile?.bio || '',
      avatarUrl: me.profile?.avatarUrl || '',
      phoneNumber: me.profile?.phoneNumber || '',
      location: me.profile?.location || '',
      languagesText: (me.profile?.languages || []).join(', ')
    };
  }
}

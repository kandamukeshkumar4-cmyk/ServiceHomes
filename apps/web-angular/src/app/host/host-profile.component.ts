import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppAuthService, HostProfile } from '../core/auth.service';

@Component({
  selector: 'app-host-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-4 max-w-60rem mx-auto">
      <div *ngIf="loading" class="surface-100 border-round-lg p-3">
        <p class="m-0">Loading host profile...</p>
      </div>

      <div *ngIf="!loading && error" class="surface-100 border-round-lg p-3 text-red-700">
        {{ error }}
      </div>

      <div *ngIf="!loading && host" class="grid gap-4">
        <div class="col-12 lg:col-4">
          <div class="surface-0 shadow-1 border-round-xl p-4">
            <div class="flex flex-column align-items-center text-center gap-3">
              <div class="w-8rem h-8rem border-circle overflow-hidden surface-200 flex align-items-center justify-content-center">
                <img *ngIf="host.avatarUrl" [src]="host.avatarUrl" class="w-full h-full" alt="host avatar" style="object-fit: cover;" />
                <span *ngIf="!host.avatarUrl" class="text-600 font-semibold">Host</span>
              </div>

              <div>
                <h1 class="text-2xl font-bold m-0">{{ host.displayName }}</h1>
                <p class="text-600 m-0" *ngIf="host.location">{{ host.location }}</p>
              </div>

              <div class="surface-100 border-round-lg p-3 w-full text-left">
                <p class="m-0"><strong>Member since:</strong> {{ host.memberSince | date:'mediumDate' }}</p>
                <p class="m-0 mt-2"><strong>Response rate:</strong> {{ host.responseRate === null ? 'Not enough data yet' : host.responseRate + '%' }}</p>
                <p class="m-0 mt-2"><strong>Published listings:</strong> {{ host.listingsCount }}</p>
                <p class="m-0 mt-2" *ngIf="host.languages.length"><strong>Languages:</strong> {{ host.languages.join(', ') }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 lg:col-8">
          <div class="surface-0 shadow-1 border-round-xl p-4 mb-4">
            <h2 class="text-xl font-bold mt-0 mb-3">About the host</h2>
            <p class="text-700 m-0" *ngIf="host.bio; else noBio">{{ host.bio }}</p>
            <ng-template #noBio>
              <p class="text-600 m-0">This host has not added a bio yet.</p>
            </ng-template>
          </div>

          <div class="surface-0 shadow-1 border-round-xl p-4">
            <div class="flex justify-content-between align-items-center mb-3">
              <h2 class="text-xl font-bold m-0">Published listings</h2>
              <span class="text-600">{{ host.listingsCount }} total</span>
            </div>

            <div *ngIf="host.listings.length; else noListings" class="grid gap-3">
              <div *ngFor="let listing of host.listings" class="col-12 md:col-6">
                <a [routerLink]="['/listings', listing.id]" class="surface-100 border-round-xl overflow-hidden block no-underline text-color">
                  <div class="h-12rem surface-200">
                    <img *ngIf="listing.coverUrl" [src]="listing.coverUrl" class="w-full h-full" alt="listing cover" style="object-fit: cover;" />
                  </div>
                  <div class="p-3">
                    <div class="text-lg font-semibold mb-1">{{ listing.title }}</div>
                    <div class="text-600 text-sm mb-2">{{ listing.city }}, {{ listing.country }}</div>
                    <div class="flex justify-content-between align-items-center">
                      <span class="font-semibold">&#36;{{ listing.nightlyPrice }} / night</span>
                      <span class="text-600 text-sm">{{ listing.bedrooms }} bd · {{ listing.maxGuests }} guests</span>
                    </div>
                  </div>
                </a>
              </div>
            </div>

            <ng-template #noListings>
              <div class="surface-100 border-round-lg p-3 text-600">
                This host does not have any published listings yet.
              </div>
            </ng-template>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class HostProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private auth = inject(AppAuthService);

  host: HostProfile | null = null;
  loading = true;
  error = '';

  ngOnInit() {
    const hostId = this.route.snapshot.paramMap.get('hostId');
    if (!hostId) {
      this.loading = false;
      this.error = 'Host not found.';
      return;
    }

    this.auth.getHostProfile(hostId).subscribe({
      next: host => {
        this.host = host;
        this.loading = false;
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to load this host profile right now.';
        this.loading = false;
      }
    });
  }
}

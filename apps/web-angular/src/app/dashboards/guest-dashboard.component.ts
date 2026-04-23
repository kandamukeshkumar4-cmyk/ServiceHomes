import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { SkeletonModule } from 'primeng/skeleton';
import { DashboardService, GuestDashboardData } from './dashboard.service';

@Component({
  selector: 'app-guest-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, BadgeModule, SkeletonModule],
  template: `
    <div class="p-4 max-w-screen-xl mx-auto">
      <h1 class="text-3xl font-bold mb-4">My Trips</h1>

      <ng-container *ngIf="data; else loading">
        <!-- Upcoming Trips -->
        <h2 class="text-xl font-semibold mb-2">Upcoming</h2>
        <div *ngIf="data.upcomingTrips.length === 0" class="text-600 mb-4">
          No upcoming trips. <a routerLink="/search" class="text-primary no-underline">Start exploring</a>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 mb-4">
          <p-card *ngFor="let trip of data.upcomingTrips">
            <div class="flex gap-3">
              <img *ngIf="trip.listingCoverUrl" [src]="trip.listingCoverUrl" class="w-6rem h-6rem border-round object-cover" />
              <div class="flex-1">
                <div class="font-semibold">{{ trip.listingTitle }}</div>
                <div class="text-sm text-600">{{ trip.listingCity }}{{ trip.listingCity && trip.listingCountry ? ', ' : '' }}{{ trip.listingCountry }}</div>
                <div class="text-sm text-600 mt-1">{{ trip.checkIn }} → {{ trip.checkOut }}</div>
                <div class="text-sm text-600">{{ trip.guests }} guests</div>
                <a [routerLink]="['/bookings', trip.reservationId]" pButton class="p-button-sm p-button-outlined mt-2">View Details</a>
              </div>
            </div>
          </p-card>
        </div>

        <!-- Past Trips -->
        <h2 class="text-xl font-semibold mb-2">Where you've been</h2>
        <div *ngIf="data.pastTrips.length === 0" class="text-600 mb-4">
          No past trips yet.
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 mb-4">
          <p-card *ngFor="let trip of data.pastTrips">
            <div class="flex gap-3">
              <img *ngIf="trip.listingCoverUrl" [src]="trip.listingCoverUrl" class="w-6rem h-6rem border-round object-cover" />
              <div class="flex-1">
                <div class="font-semibold">{{ trip.listingTitle }}</div>
                <div class="text-sm text-600">{{ trip.checkIn }} → {{ trip.checkOut }}</div>
                <div *ngIf="trip.canReview" class="mt-2">
                  <a [routerLink]="['/bookings', trip.reservationId]" pButton class="p-button-sm p-button-primary">Leave a Review</a>
                </div>
                <div *ngIf="!trip.canReview" class="mt-2">
                  <a [routerLink]="['/bookings', trip.reservationId]" pButton class="p-button-sm p-button-outlined">View Details</a>
                </div>
              </div>
            </div>
          </p-card>
        </div>

        <!-- Saved Preview -->
        <div class="flex align-items-center justify-content-between mb-2">
          <h2 class="text-xl font-semibold">Saved</h2>
          <a routerLink="/saved" class="text-primary no-underline text-sm">View all ({{ data.savedListingsCount }})</a>
        </div>
        <p-card *ngIf="data.savedListingsCount === 0" class="mb-4">
          <div class="text-600">No saved listings yet.</div>
        </p-card>

        <!-- Inbox Preview -->
        <div class="flex align-items-center justify-content-between mb-2">
          <h2 class="text-xl font-semibold">Messages</h2>
          <a routerLink="/inbox" class="text-primary no-underline text-sm">
            View inbox
            <span *ngIf="data.unreadMessageThreads > 0" pBadge [value]="data.unreadMessageThreads" severity="danger"></span>
          </a>
        </div>
      </ng-container>

      <ng-template #loading>
        <p-skeleton height="1.5rem" width="10rem" class="mb-2"></p-skeleton>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
          <p-skeleton height="10rem" *ngFor="let _ of [1,2,3]"></p-skeleton>
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    .object-cover { object-fit: cover; }
  `]
})
export class GuestDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  data: GuestDashboardData | null = null;

  ngOnInit(): void {
    this.dashboardService.getGuestDashboard().subscribe({
      next: (d) => this.data = d,
      error: (err) => console.error('Failed to load guest dashboard', err)
    });
  }
}

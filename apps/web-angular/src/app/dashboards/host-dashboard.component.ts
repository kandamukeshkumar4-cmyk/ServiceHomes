import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { TableModule } from 'primeng/table';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';
import { DashboardService, HostDashboardData } from './dashboard.service';

@Component({
  selector: 'app-host-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, BadgeModule, TableModule, ProgressBarModule, SkeletonModule],
  template: `
    <div class="p-4 max-w-screen-xl mx-auto">
      <h1 class="text-3xl font-bold mb-4">Host Dashboard</h1>

      <ng-container *ngIf="data; else loading">
        <!-- Quick Stats -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4">
          <p-card>
            <div class="text-sm text-600">Pending Requests</div>
            <div class="text-2xl font-bold">{{ data.pendingRequests.length }}</div>
          </p-card>
          <p-card>
            <div class="text-sm text-600">Upcoming Check-ins</div>
            <div class="text-2xl font-bold">{{ data.upcomingReservations.length }}</div>
          </p-card>
          <p-card>
            <div class="text-sm text-600">30-Day Occupancy</div>
            <div class="text-2xl font-bold">{{ fmtPct(data.occupancyRate) }}%</div>
          </p-card>
          <p-card>
            <div class="text-sm text-600">Earnings (This Month)</div>
            <div class="text-2xl font-bold">${{ fmtNum(data.mockEarnings) }}</div>
          </p-card>
        </div>

        <!-- Reservation Pipeline -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
          <p-card header="Pending Requests">
            <div *ngIf="data.pendingRequests.length === 0" class="text-600 text-center py-4">
              No pending requests
            </div>
            <div *ngFor="let req of data.pendingRequests" class="flex align-items-center gap-3 p-2 border-bottom-1 surface-border">
              <img *ngIf="req.listingCoverUrl" [src]="req.listingCoverUrl" class="w-4rem h-4rem border-round object-cover" />
              <div class="flex-1">
                <div class="font-semibold">{{ req.listingTitle }}</div>
                <div class="text-sm text-600">{{ req.guestDisplayName }} · {{ req.checkIn }} → {{ req.checkOut }}</div>
              </div>
              <div class="flex gap-2">
                <a [routerLink]="['/bookings', req.reservationId]" pButton class="p-button-sm p-button-outlined">View</a>
              </div>
            </div>
          </p-card>

          <p-card header="Upcoming Reservations">
            <div *ngIf="data.upcomingReservations.length === 0" class="text-600 text-center py-4">
              No upcoming reservations
            </div>
            <div *ngFor="let res of data.upcomingReservations" class="flex align-items-center gap-3 p-2 border-bottom-1 surface-border">
              <img *ngIf="res.listingCoverUrl" [src]="res.listingCoverUrl" class="w-4rem h-4rem border-round object-cover" />
              <div class="flex-1">
                <div class="font-semibold">{{ res.listingTitle }}</div>
                <div class="text-sm text-600">{{ res.checkIn }} → {{ res.checkOut }} · {{ res.guestDisplayName }}</div>
              </div>
              <a [routerLink]="['/bookings', res.reservationId]" pButton class="p-button-sm p-button-outlined">View</a>
            </div>
          </p-card>
        </div>

        <!-- Listing Performance -->
        <p-card header="Listing Performance" class="mb-4">
          <p-table [value]="data.listingPerformance" [paginator]="true" [rows]="5">
            <ng-template pTemplate="header">
              <tr>
                <th>Listing</th>
                <th>Bookings</th>
                <th>Rating</th>
                <th>Reviews</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-item>
              <tr>
                <td>
                  <div class="flex align-items-center gap-2">
                    <img *ngIf="item.coverUrl" [src]="item.coverUrl" class="w-3rem h-3rem border-round object-cover" />
                    <span>{{ item.listingTitle }}</span>
                  </div>
                </td>
                <td>{{ item.bookingCount }}</td>
                <td>
                  <span *ngIf="item.averageRating">{{ item.averageRating.toFixed(1) }} ★</span>
                  <span *ngIf="!item.averageRating" class="text-600">—</span>
                </td>
                <td>{{ item.reviewCount }}</td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>

        <!-- Quick Actions -->
        <div class="flex gap-2">
          <a routerLink="/listings/new" pButton class="p-button-primary">Add New Listing</a>
            <a routerLink="/inbox" pButton class="p-button-outlined">
            Inbox
            <span *ngIf="data.unreadMessageThreads > 0" pBadge [value]="data.unreadMessageThreads" severity="danger"></span>
          </a>
        </div>
      </ng-container>

      <ng-template #loading>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4">
          <p-skeleton height="5rem" *ngFor="let _ of [1,2,3,4]"></p-skeleton>
        </div>
        <p-skeleton height="20rem" class="mb-4"></p-skeleton>
      </ng-template>
    </div>
  `,
  styles: [`
    .object-cover { object-fit: cover; }
  `]
})
export class HostDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  data: HostDashboardData | null = null;

  ngOnInit(): void {
    this.dashboardService.getHostDashboard().subscribe({
      next: (d) => this.data = d,
      error: (err) => console.error('Failed to load host dashboard', err)
    });
  }

  fmtNum(n: number): string {
    return Math.round(n).toLocaleString();
  }

  fmtPct(n: number): string {
    return Math.round(n).toString();
  }
}

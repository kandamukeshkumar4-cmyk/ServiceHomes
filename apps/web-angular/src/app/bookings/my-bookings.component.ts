import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';

interface Booking {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string;
  listingCity: string;
  listingCountry: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  totalAmount: number;
  status: string;
  hostDisplayName: string;
}

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-4 max-w-60rem mx-auto">
      <h1 class="text-2xl font-bold mb-4">My Bookings</h1>

      <div *ngIf="bookings.length === 0" class="surface-100 p-4 border-round-lg text-center">
        <p class="text-600">You have no bookings yet.</p>
        <a routerLink="/home" class="p-button p-button-primary no-underline mt-2 inline-block">Browse listings</a>
      </div>

      <div class="flex flex-column gap-3">
        <div *ngFor="let booking of bookings" class="surface-0 shadow-1 border-round-lg overflow-hidden flex flex-column md:flex-row">
          <div class="h-10rem md:h-auto md:w-15rem surface-200 flex-shrink-0">
            <img *ngIf="booking.listingCoverUrl" [src]="booking.listingCoverUrl" class="w-full h-full object-cover" />
          </div>
          <div class="p-3 flex-1 flex flex-column justify-content-between">
            <div>
              <div class="flex justify-content-between align-items-start mb-1">
                <h3 class="text-lg font-bold m-0">{{ booking.listingTitle }}</h3>
                <span class="px-2 py-1 text-xs border-round-lg"
                      [class.bg-green-100]="booking.status === 'CONFIRMED'"
                      [class.text-green-700]="booking.status === 'CONFIRMED'"
                      [class.bg-yellow-100]="booking.status === 'PENDING'"
                      [class.text-yellow-700]="booking.status === 'PENDING'"
                      [class.bg-red-100]="booking.status.includes('CANCELLED')"
                      [class.text-red-700]="booking.status.includes('CANCELLED')">
                  {{ booking.status }}
                </span>
              </div>
              <p class="text-600 text-sm m-0">{{ booking.listingCity }}, {{ booking.listingCountry }}</p>
              <p class="text-600 text-sm m-0 mt-1">{{ booking.checkIn }} to {{ booking.checkOut }} · {{ booking.guests }} guests · {{ booking.totalNights }} nights</p>
              <p class="text-600 text-sm m-0 mt-1">Hosted by {{ booking.hostDisplayName }}</p>
            </div>
            <div class="flex justify-content-between align-items-center mt-2">
              <span class="font-bold">${{ booking.totalAmount }} total</span>
              <button *ngIf="canCancel(booking.status)" class="p-button p-button-sm p-button-danger" (click)="cancel(booking.id)">Cancel</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class MyBookingsComponent implements OnInit {
  private http = inject(HttpClient);
  bookings: Booking[] = [];

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<Booking[]>('/api/reservations/my').subscribe(data => this.bookings = data);
  }

  canCancel(status: string): boolean {
    return status === 'PENDING' || status === 'CONFIRMED';
  }

  cancel(id: string) {
    this.http.post(`/api/reservations/${id}/cancel`, {}).subscribe(() => this.load());
  }
}

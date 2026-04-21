import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface BookingDetail {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string;
  listingCity: string;
  listingCountry: string;
  guestId: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  nightlyPrice: number;
  cleaningFee: number;
  serviceFee: number;
  totalAmount: number;
  status: string;
  hostDisplayName: string;
}

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-4 max-w-40rem mx-auto" *ngIf="booking">
      <a routerLink="/bookings" class="text-600 no-underline mb-2 inline-block">← Back to bookings</a>
      <h1 class="text-2xl font-bold mb-1">Booking details</h1>

      <div class="surface-0 shadow-1 border-round-lg overflow-hidden mb-3">
        <div class="h-12rem surface-200">
          <img *ngIf="booking.listingCoverUrl" [src]="booking.listingCoverUrl" class="w-full h-full object-cover" />
        </div>
        <div class="p-3">
          <h2 class="text-xl font-bold m-0">{{ booking.listingTitle }}</h2>
          <p class="text-600 m-0">{{ booking.listingCity }}, {{ booking.listingCountry }}</p>
        </div>
      </div>

      <div class="surface-0 shadow-1 border-round-lg p-3 mb-3">
        <h3 class="text-lg font-bold mb-2">Trip details</h3>
        <div class="flex flex-column gap-2">
          <div class="flex justify-content-between">
            <span class="text-600">Dates</span>
            <span>{{ booking.checkIn }} to {{ booking.checkOut }}</span>
          </div>
          <div class="flex justify-content-between">
            <span class="text-600">Guests</span>
            <span>{{ booking.guests }}</span>
          </div>
          <div class="flex justify-content-between">
            <span class="text-600">Nights</span>
            <span>{{ booking.totalNights }}</span>
          </div>
          <div class="flex justify-content-between">
            <span class="text-600">Status</span>
            <span class="font-bold" [class.text-green-700]="booking.status === 'CONFIRMED'"
                  [class.text-yellow-700]="booking.status === 'PENDING'"
                  [class.text-red-700]="booking.status.includes('CANCELLED')">
              {{ booking.status }}
            </span>
          </div>
        </div>
      </div>

      <div class="surface-0 shadow-1 border-round-lg p-3 mb-3">
        <h3 class="text-lg font-bold mb-2">Price breakdown</h3>
        <div class="flex flex-column gap-2">
          <div class="flex justify-content-between">
            <span class="text-600">${{ booking.nightlyPrice }} x {{ booking.totalNights }} nights</span>
            <span>${{ booking.nightlyPrice * booking.totalNights }}</span>
          </div>
          <div class="flex justify-content-between">
            <span class="text-600">Cleaning fee</span>
            <span>${{ booking.cleaningFee }}</span>
          </div>
          <div class="flex justify-content-between">
            <span class="text-600">Service fee</span>
            <span>${{ booking.serviceFee }}</span>
          </div>
          <div class="flex justify-content-between border-top-1 surface-border pt-2 mt-2 font-bold">
            <span>Total</span>
            <span>${{ booking.totalAmount }}</span>
          </div>
        </div>
      </div>

      <div class="flex gap-2">
        <a [routerLink]="['/listings', booking.listingId]" class="p-button p-button-outlined no-underline flex-1 text-center">View listing</a>
        <button *ngIf="canCancel()" class="p-button p-button-danger flex-1" (click)="cancel()">Cancel booking</button>
      </div>
    </div>
  `,
  styles: []
})
export class BookingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private router = inject(Router);

  booking: BookingDetail | null = null;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.http.get<BookingDetail>(`/api/reservations/${id}`).subscribe(b => this.booking = b);
  }

  canCancel(): boolean {
    return this.booking ? ['PENDING', 'CONFIRMED'].includes(this.booking.status) : false;
  }

  cancel() {
    if (!this.booking) return;
    this.http.post(`/api/reservations/${this.booking.id}/cancel`, {}).subscribe(() => {
      this.router.navigate(['/bookings']);
    });
  }
}

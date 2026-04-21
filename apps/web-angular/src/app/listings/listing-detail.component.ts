import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ListingService } from '../listings/listing.service';
import { Listing, ListingPhoto } from '../listings/listing.model';
import { AppAuthService } from '../core/auth.service';

interface Quote {
  totalNights: number;
  nightlyPrice: number;
  subtotal: number;
  cleaningFee: number;
  serviceFee: number;
  totalAmount: number;
}

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-4 max-w-60rem mx-auto" *ngIf="listing">
      <h1 class="text-2xl font-bold mb-1">{{ listing.title }}</h1>
      <p class="text-600 mb-3">{{ listing.location.city }}, {{ listing.location.country }}</p>

      <div class="grid gap-2 mb-4">
        <div class="col-12 md:col-8">
          <div class="h-20rem surface-200 border-round-lg overflow-hidden">
            <img *ngIf="coverPhoto" [src]="coverPhoto.url" class="w-full h-full object-cover" />
          </div>
        </div>
        <div class="col-12 md:col-4 flex flex-column gap-2">
          <div *ngFor="let photo of otherPhotos" class="h-6rem surface-200 border-round-lg overflow-hidden">
            <img [src]="photo.url" class="w-full h-full object-cover" />
          </div>
        </div>
      </div>

      <div class="grid">
        <div class="col-12 md:col-8">
          <div class="surface-0 shadow-1 border-round-lg p-3 mb-3">
            <h2 class="text-xl font-bold mb-2">About this place</h2>
            <p class="text-600">{{ listing.description }}</p>
            <div class="flex gap-3 mt-3">
              <span class="px-2 py-1 surface-100 border-round-lg text-sm">{{ listing.propertyType }}</span>
              <span class="px-2 py-1 surface-100 border-round-lg text-sm">{{ listing.maxGuests }} guests</span>
              <span class="px-2 py-1 surface-100 border-round-lg text-sm">{{ listing.bedrooms }} bedrooms</span>
              <span class="px-2 py-1 surface-100 border-round-lg text-sm">{{ listing.beds }} beds</span>
              <span class="px-2 py-1 surface-100 border-round-lg text-sm">{{ listing.bathrooms }} baths</span>
            </div>
          </div>

          <div class="surface-0 shadow-1 border-round-lg p-3 mb-3">
            <h2 class="text-xl font-bold mb-2">What this place offers</h2>
            <div class="flex flex-wrap gap-2">
              <span *ngFor="let amenity of listing.amenities" class="px-3 py-2 surface-100 border-round-lg text-sm">{{ amenity.name }}</span>
            </div>
          </div>
        </div>

        <div class="col-12 md:col-4">
          <div class="surface-0 shadow-1 border-round-lg p-3 sticky top-4">
            <div class="flex justify-content-between align-items-center mb-3">
              <span class="text-xl font-bold">${{ listing.nightlyPrice }}</span>
              <span class="text-600">/ night</span>
            </div>
            <div class="flex flex-column gap-2 mb-3">
              <div class="grid gap-1">
                <div class="col-6 flex flex-column gap-1">
                  <label class="text-xs font-bold">Check in</label>
                  <input type="date" class="p-inputtext" [(ngModel)]="checkIn" />
                </div>
                <div class="col-6 flex flex-column gap-1">
                  <label class="text-xs font-bold">Check out</label>
                  <input type="date" class="p-inputtext" [(ngModel)]="checkOut" />
                </div>
              </div>
              <div class="flex flex-column gap-1">
                <label class="text-xs font-bold">Guests</label>
                <input type="number" class="p-inputtext" [max]="listing.maxGuests" min="1" [(ngModel)]="guests" />
              </div>
            </div>

            <div *ngIf="priceQuote" class="mb-3">
              <div class="flex justify-content-between text-sm mb-1">
                <span>${{ priceQuote.nightlyPrice }} x {{ priceQuote.totalNights }} nights</span>
                <span>${{ priceQuote.subtotal }}</span>
              </div>
              <div class="flex justify-content-between text-sm mb-1">
                <span>Cleaning fee</span>
                <span>${{ priceQuote.cleaningFee }}</span>
              </div>
              <div class="flex justify-content-between text-sm mb-1">
                <span>Service fee</span>
                <span>${{ priceQuote.serviceFee }}</span>
              </div>
              <div class="flex justify-content-between font-bold border-top-1 surface-border pt-2 mt-2">
                <span>Total</span>
                <span>${{ priceQuote.totalAmount }}</span>
              </div>
            </div>

            <button class="p-button p-button-primary w-full" (click)="reserve()" [disabled]="!auth.isAuthenticated$">
              {{ (auth.isAuthenticated$ | async) ? 'Reserve' : 'Log in to reserve' }}
            </button>
            <div class="mt-3 text-center text-600 text-sm">You won't be charged yet</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ListingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);
  private http = inject(HttpClient);
  auth = inject(AppAuthService);

  listing: Listing | null = null;
  checkIn = '';
  checkOut = '';
  guests = 1;
  priceQuote: Quote | null = null;

  get coverPhoto(): ListingPhoto | undefined {
    return this.listing?.photos.find(p => p.isCover) || this.listing?.photos[0];
  }

  get otherPhotos(): ListingPhoto[] {
    return (this.listing?.photos || []).filter(p => p.id !== this.coverPhoto?.id).slice(0, 3);
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.listingService.getById(id).subscribe(l => {
      this.listing = l;
    });
  }

  reserve() {
    if (!this.listing || !this.checkIn || !this.checkOut) return;
    this.http.post('/api/reservations', {
      listingId: this.listing.id,
      checkIn: this.checkIn,
      checkOut: this.checkOut,
      guests: this.guests
    }).subscribe(() => {
      alert('Reservation created!');
    });
  }
}

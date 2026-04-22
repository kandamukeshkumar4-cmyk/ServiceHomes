import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ListingService } from '../listings/listing.service';
import { Listing, ListingPhoto } from '../listings/listing.model';
import { AppAuthService } from '../core/auth.service';
import { ListingMapComponent } from './listing-map.component';
import { ReservationQuote, ReservationRecord } from '../bookings/reservation.model';


@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ListingMapComponent, RouterLink],
  templateUrl: './listing-detail.component.html',
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
  quoteNights = 0;
  quoteNightlyPrice = 0;
  quoteSubtotal = 0;
  quoteCleaningFee = 0;
  quoteServiceFee = 0;
  quoteTotal = 0;
  bookingStep: 'dates' | 'checkout' | 'complete' = 'dates';
  bookingError = '';
  reviewingQuote = false;
  creatingReservation = false;
  createdReservation: ReservationRecord | null = null;

  get coverPhoto(): ListingPhoto | undefined {
    return this.listing?.photos.find(p => p.isCover) || this.listing?.photos[0];
  }

  get otherPhotos(): ListingPhoto[] {
    return (this.listing?.photos || []).filter(p => p.id !== this.coverPhoto?.id).slice(0, 3);
  }

  get requiresHostApproval(): boolean {
    return !this.listing?.policy?.instantBook;
  }

  get reviewActionLabel(): string {
    return this.requiresHostApproval ? 'Review booking request' : 'Review instant booking';
  }

  get confirmActionLabel(): string {
    return this.requiresHostApproval ? 'Request to book' : 'Confirm instant booking';
  }

  get completionTitle(): string {
    if (!this.createdReservation) {
      return '';
    }
    return this.createdReservation.status === 'CONFIRMED' ? 'Booking confirmed' : 'Booking request sent';
  }

  get completionMessage(): string {
    if (!this.createdReservation) {
      return '';
    }
    if (this.createdReservation.status === 'CONFIRMED') {
      return 'Your stay is confirmed. No payment was collected in this demo checkout.';
    }
    return 'The host can now accept or decline your request. No payment was collected in this demo checkout.';
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.listingService.getById(id).subscribe(l => {
      this.listing = l;
    });
  }

  reviewBooking() {
    if (!this.listing || !this.checkIn || !this.checkOut) {
      this.bookingError = 'Choose check-in and check-out dates to continue.';
      return;
    }
    this.bookingError = '';
    this.reviewingQuote = true;
    this.http.post<ReservationQuote>('/api/reservations/quote', {
      listingId: this.listing.id,
      checkIn: this.checkIn,
      checkOut: this.checkOut,
      guests: this.guests
    }).subscribe({
      next: quote => {
        this.quoteNights = quote.totalNights;
        this.quoteNightlyPrice = quote.nightlyPrice;
        this.quoteSubtotal = quote.subtotal;
        this.quoteCleaningFee = quote.cleaningFee;
        this.quoteServiceFee = quote.serviceFee;
        this.quoteTotal = quote.totalAmount;
        this.bookingStep = 'checkout';
        this.reviewingQuote = false;
      },
      error: error => {
        this.bookingError = error?.error?.message || 'Unable to review this booking right now.';
        this.reviewingQuote = false;
      }
    });
  }

  editTrip() {
    this.bookingStep = 'dates';
    this.bookingError = '';
    this.createdReservation = null;
  }

  reserve() {
    if (!this.listing || !this.checkIn || !this.checkOut) {
      return;
    }
    this.bookingError = '';
    this.creatingReservation = true;
    this.http.post<ReservationRecord>('/api/reservations', {
      listingId: this.listing.id,
      checkIn: this.checkIn,
      checkOut: this.checkOut,
      guests: this.guests
    }).subscribe({
      next: reservation => {
        this.createdReservation = reservation;
        this.bookingStep = 'complete';
        this.creatingReservation = false;
      },
      error: error => {
        this.bookingError = error?.error?.message || 'Unable to create your reservation.';
        this.creatingReservation = false;
      }
    });
  }
}

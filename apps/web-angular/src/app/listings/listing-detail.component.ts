import { Component, DestroyRef, ViewChild, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ListingService } from '../listings/listing.service';
import { Listing, ListingPhoto } from '../listings/listing.model';
import { AppAuthService } from '../core/auth.service';
import { ListingMapComponent } from './listing-map.component';
import { ReservationQuote, ReservationRecord } from '../bookings/reservation.model';
import { ReviewFormComponent, ReviewSubmission } from '../reviews/review-form.component';
import { ReviewListComponent } from '../reviews/review-list.component';
import { ListingReview, ReviewReservationOption, ReviewsApiService } from '../reviews/reviews-api.service';


@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ListingMapComponent, RouterLink, ReviewFormComponent, ReviewListComponent],
  templateUrl: './listing-detail.component.html',
  styles: []
})
export class ListingDetailComponent implements OnInit {
  @ViewChild(ReviewFormComponent) reviewForm?: ReviewFormComponent;

  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);
  private http = inject(HttpClient);
  private reviewsApi = inject(ReviewsApiService);
  auth = inject(AppAuthService);

  listing: Listing | null = null;
  currentUserId: string | null = null;
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
  reviews: ListingReview[] = [];
  averageRating = 0;
  reviewCount = 0;
  reviewsLoading = false;
  reviewsLoaded = false;
  reviewsError = '';
  eligibleReservations: ReviewReservationOption[] = [];
  eligibleReservationsLoading = false;
  reviewSubmitError = '';
  reviewSubmitting = false;
  hostResponseError = '';
  hostResponseSubmittingFor: string | null = null;

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

  get isHostViewer(): boolean {
    return !!this.currentUserId && this.currentUserId === this.listing?.hostId;
  }

  get canLeaveReview(): boolean {
    return !!this.currentUserId && !this.isHostViewer && this.eligibleReservations.length > 0;
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.auth.me
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(me => {
        this.currentUserId = me?.id ?? null;
        this.loadEligibleReservations();
      });

    this.listingService.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(l => {
        this.listing = l;
        this.loadReviews();
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

  submitReview(submission: ReviewSubmission) {
    this.reviewSubmitError = '';
    this.reviewSubmitting = true;

    this.reviewsApi.createReview(submission.reservationId, submission.rating, submission.comment)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.reviewSubmitError = '';
          this.reviewSubmitting = false;
          this.reviewForm?.reset();
          this.loadReviews();
        },
        error: error => {
          this.reviewSubmitError = error?.error?.message || 'Unable to publish your review.';
          this.reviewSubmitting = false;
        }
      });
  }

  submitHostResponse(event: { reviewId: string; response: string }) {
    this.hostResponseError = '';
    this.hostResponseSubmittingFor = event.reviewId;

    this.reviewsApi.addHostResponse(event.reviewId, event.response)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updatedReview => {
          this.hostResponseError = '';
          this.reviews = this.reviews.map(review => review.id === updatedReview.id ? updatedReview : review);
          this.hostResponseSubmittingFor = null;
        },
        error: error => {
          this.hostResponseError = error?.error?.message || 'Unable to save the host response.';
          this.hostResponseSubmittingFor = null;
        }
      });
  }

  private loadReviews() {
    if (!this.listing) {
      return;
    }

    this.reviewsLoading = true;
    this.reviewsError = '';

    this.reviewsApi.getListingReviews(this.listing.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.averageRating = response.averageRating;
          this.reviewCount = response.reviewCount;
          this.reviews = response.content;
          this.reviewsLoading = false;
          this.reviewsLoaded = true;
          this.loadEligibleReservations();
        },
        error: error => {
          this.reviewsError = error?.error?.message || 'Unable to load reviews right now.';
          this.reviewsLoading = false;
          this.reviewsLoaded = true;
          this.loadEligibleReservations();
        }
      });
  }

  private loadEligibleReservations() {
    if (!this.listing || !this.currentUserId || this.isHostViewer || !this.reviewsLoaded) {
      this.eligibleReservationsLoading = false;
      if (this.isHostViewer) {
        this.eligibleReservations = [];
      }
      return;
    }

    this.eligibleReservationsLoading = true;

    this.reviewsApi.getEligibleReservations(
      this.listing.id,
      this.reviews.map(review => review.reservationId)
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: reservations => {
          this.eligibleReservations = reservations;
          this.eligibleReservationsLoading = false;
        },
        error: () => {
          this.eligibleReservations = [];
          this.eligibleReservationsLoading = false;
        }
      });
  }
}

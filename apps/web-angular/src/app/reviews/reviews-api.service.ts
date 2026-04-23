import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { ReservationPage } from '../bookings/reservation.model';

export interface ListingReview {
  id: string;
  reservationId: string;
  listingId: string;
  guestId: string;
  hostId: string;
  reviewerId: string;
  reviewerRole: 'GUEST' | 'HOST';
  rating: number;
  cleanlinessRating: number | null;
  accuracyRating: number | null;
  communicationRating: number | null;
  locationRating: number | null;
  valueRating: number | null;
  comment: string;
  guestDisplayName: string;
  guestAvatarUrl: string | null;
  hostResponse: string | null;
  visibleAt: string;
  moderationStatus: 'APPROVED' | 'HIDDEN';
  reportCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ListingReviewsResponse {
  averageRating: number;
  reviewCount: number;
  cleanlinessRating: number | null;
  accuracyRating: number | null;
  communicationRating: number | null;
  locationRating: number | null;
  valueRating: number | null;
  trustScore: number;
  content: ListingReview[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ReviewReservationOption {
  id: string;
  checkIn: string;
  checkOut: string;
  status: string;
  summary: string;
}

export interface ReviewRatingBreakdown {
  cleanlinessRating: number;
  accuracyRating: number;
  communicationRating: number;
  locationRating: number;
  valueRating: number;
}

export interface ReviewReport {
  id: string;
  reviewId: string;
  reporterId: string;
  reason: string;
  details: string | null;
  status: string;
  createdAt: string;
  resolvedAt: string | null;
  resolvedBy: string | null;
}

@Injectable({ providedIn: 'root' })
export class ReviewsApiService {
  private http = inject(HttpClient);

  getListingReviews(listingId: string, page = 0, size = 50): Observable<ListingReviewsResponse> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<ListingReviewsResponse>(`/api/listings/${listingId}/reviews`, { params });
  }

  createReview(
    reservationId: string,
    rating: number,
    breakdown: ReviewRatingBreakdown,
    comment: string
  ): Observable<ListingReview> {
    return this.http.post<ListingReview>(`/api/reservations/${reservationId}/review`, {
      rating,
      ...breakdown,
      comment
    });
  }

  addHostResponse(reviewId: string, response: string): Observable<ListingReview> {
    return this.http.post<ListingReview>(`/api/reviews/${reviewId}/response`, { response });
  }

  reportReview(reviewId: string, reason: string, details: string): Observable<ReviewReport> {
    return this.http.post<ReviewReport>(`/api/reviews/${reviewId}/report`, {
      reason,
      details: details.trim() || null
    });
  }

  getEligibleReservations(listingId: string, reviewedReservationIds: string[]): Observable<ReviewReservationOption[]> {
    const params = new HttpParams().set('size', 100);
    const today = new Date().toISOString().slice(0, 10);
    const reviewedIds = new Set(reviewedReservationIds);

    return this.http.get<ReservationPage>('/api/reservations/my', { params }).pipe(
      map(page => page.content
        .filter(reservation =>
          reservation.listingId === listingId
          && reservation.status === 'COMPLETED'
          && reservation.checkOut < today
          && !reviewedIds.has(reservation.id)
        )
        .sort((left, right) => right.checkOut.localeCompare(left.checkOut))
        .map(reservation => ({
          id: reservation.id,
          checkIn: reservation.checkIn,
          checkOut: reservation.checkOut,
          status: reservation.status,
          summary: `${reservation.checkIn} to ${reservation.checkOut}`
        }))
      )
    );
  }
}

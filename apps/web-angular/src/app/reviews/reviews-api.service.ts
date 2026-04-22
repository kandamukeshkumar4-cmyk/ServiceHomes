import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { ReservationPage } from '../bookings/reservation.model';

export interface ListingReview {
  id: string;
  reservationId: string;
  listingId: string;
  guestId: string;
  rating: number;
  comment: string;
  guestDisplayName: string;
  guestAvatarUrl: string | null;
  hostResponse: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ListingReviewsResponse {
  averageRating: number;
  reviewCount: number;
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

@Injectable({ providedIn: 'root' })
export class ReviewsApiService {
  private http = inject(HttpClient);

  getListingReviews(listingId: string, page = 0, size = 50): Observable<ListingReviewsResponse> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<ListingReviewsResponse>(`/api/listings/${listingId}/reviews`, { params });
  }

  createReview(reservationId: string, rating: number, comment: string): Observable<ListingReview> {
    return this.http.post<ListingReview>(`/api/reservations/${reservationId}/review`, {
      rating,
      comment
    });
  }

  addHostResponse(reviewId: string, response: string): Observable<ListingReview> {
    return this.http.post<ListingReview>(`/api/reviews/${reviewId}/response`, { response });
  }

  getEligibleReservations(listingId: string, reviewedReservationIds: string[]): Observable<ReviewReservationOption[]> {
    const params = new HttpParams().set('size', 100);
    const today = new Date().toISOString().slice(0, 10);
    const reviewedIds = new Set(reviewedReservationIds);

    return this.http.get<ReservationPage>('/api/reservations/my', { params }).pipe(
      map(page => page.content
        .filter(reservation =>
          reservation.listingId === listingId
          && ['CONFIRMED', 'COMPLETED'].includes(reservation.status)
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

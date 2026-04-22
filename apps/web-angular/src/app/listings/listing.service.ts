import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Listing,
  CreateListingPayload,
  ListingCategory,
  ListingAmenity,
  ListingAvailabilityResponse,
  ListingAvailabilityRule,
  ListingCalendarResponse,
  ListingPage
} from './listing.model';

@Injectable({ providedIn: 'root' })
export class ListingService {
  private http = inject(HttpClient);

  getMyListings(): Observable<ListingPage> {
    return this.http.get<ListingPage>('/api/listings/my');
  }

  getById(id: string): Observable<Listing> {
    return this.http.get<Listing>(`/api/listings/${id}`);
  }

  create(payload: CreateListingPayload): Observable<Listing> {
    return this.http.post<Listing>('/api/listings', payload);
  }

  update(id: string, payload: CreateListingPayload): Observable<Listing> {
    return this.http.put<Listing>(`/api/listings/${id}`, payload);
  }

  publish(id: string): Observable<Listing> {
    return this.http.post<Listing>(`/api/listings/${id}/publish`, {});
  }

  unpublish(id: string): Observable<Listing> {
    return this.http.post<Listing>(`/api/listings/${id}/unpublish`, {});
  }

  getAvailability(id: string): Observable<ListingAvailabilityResponse> {
    return this.http.get<ListingAvailabilityResponse>(`/api/listings/${id}/availability`);
  }

  updateAvailability(id: string, rules: ListingAvailabilityRule[]): Observable<ListingAvailabilityResponse> {
    return this.http.put<ListingAvailabilityResponse>(`/api/listings/${id}/availability`, { rules });
  }

  getCalendar(id: string, startDate?: string, endDate?: string): Observable<ListingCalendarResponse> {
    const params = new URLSearchParams();
    if (startDate) {
      params.set('startDate', startDate);
    }
    if (endDate) {
      params.set('endDate', endDate);
    }
    const query = params.toString();
    return this.http.get<ListingCalendarResponse>(`/api/listings/${id}/calendar${query ? `?${query}` : ''}`);
  }

  getCategories(): Observable<ListingCategory[]> {
    // Will be added to backend later; mock for now
    return this.http.get<ListingCategory[]>('/api/listings/categories');
  }

  getAmenities(): Observable<ListingAmenity[]> {
    // Will be added to backend later; mock for now
    return this.http.get<ListingAmenity[]>('/api/listings/amenities');
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SearchFilters } from '../search/search-filters.model';
import {
  Listing,
  CreateListingPayload,
  ListingCategory,
  ListingAmenity,
  ListingAvailabilityResponse,
  ListingAvailabilityRule,
  ListingCalendarResponse,
  ListingPage,
  ListingSearchPage
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

  search(filters: SearchFilters): Observable<ListingSearchPage> {
    const params = new URLSearchParams();
    if (filters.locationQuery) params.set('locationQuery', filters.locationQuery);
    if (filters.checkIn) params.set('checkIn', filters.checkIn);
    if (filters.checkOut) params.set('checkOut', filters.checkOut);
    if (filters.guests > 1) params.set('guests', String(filters.guests));
    if (filters.minPrice !== null) params.set('minPrice', String(filters.minPrice));
    if (filters.maxPrice !== null) params.set('maxPrice', String(filters.maxPrice));
    if (filters.lat !== null) params.set('lat', String(filters.lat));
    if (filters.lng !== null) params.set('lng', String(filters.lng));
    if (filters.radiusKm !== null) params.set('radiusKm', String(filters.radiusKm));
    if (filters.bedrooms !== null) params.set('bedrooms', String(filters.bedrooms));
    if (filters.instantBook) params.set('instantBook', 'true');
    if (filters.swLat !== null) params.set('swLat', String(filters.swLat));
    if (filters.swLng !== null) params.set('swLng', String(filters.swLng));
    if (filters.neLat !== null) params.set('neLat', String(filters.neLat));
    if (filters.neLng !== null) params.set('neLng', String(filters.neLng));
    if (filters.propertyTypes.length > 0) {
      filters.propertyTypes.forEach((propertyType) => params.append('propertyTypes', propertyType.toUpperCase()));
    }

    const sortMapping: Record<string, string> = {
      relevance: 'RELEVANCE',
      priceAsc: 'PRICE_ASC',
      priceDesc: 'PRICE_DESC',
      newest: 'NEWEST',
      ratingDesc: 'RATING_DESC'
    };
    params.set('sort', sortMapping[filters.sort] ?? 'RELEVANCE');
    params.set('page', String(filters.page));
    params.set('size', String(filters.size));

    return this.http.get<ListingSearchPage>(`/api/listings/search?${params.toString()}`);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { SearchFilters } from '../search/search-filters.model';
import { ListingSearchResult } from '../listings/listing.model';

export interface SearchSuggestion {
  text: string;
  type: string;
  subtitle: string;
}

export interface SearchApiResponse {
  content: ListingSearchResult[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
  cursor: string | null;
  searchQueryId: string | null;
}

@Injectable({ providedIn: 'root' })
export class SearchApiService {
  private http = inject(HttpClient);

  search(filters: SearchFilters): Observable<SearchApiResponse> {
    const body = this.buildSearchBody(filters);
    return this.http.post<SearchApiResponse>('/api/listings/search', body);
  }

  getSuggestions(query: string): Observable<SearchSuggestion[]> {
    if (!query || query.trim().length < 2) {
      return of([]);
    }
    return this.http.get<SearchSuggestion[]>('/api/listings/search/suggestions', {
      params: { q: query.trim() }
    });
  }

  recordClick(searchQueryId: string, listingId: string, resultPosition: number): Observable<void> {
    return this.http.post<void>('/api/listings/search/click', {
      searchQueryId,
      listingId,
      resultPosition
    });
  }

  private buildSearchBody(filters: SearchFilters): Record<string, unknown> {
    const body: Record<string, unknown> = {
      query: filters.locationQuery || null,
      guests: filters.guests > 1 ? filters.guests : null,
      checkIn: filters.checkIn || null,
      checkOut: filters.checkOut || null,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      lat: filters.lat,
      lng: filters.lng,
      radiusKm: filters.radiusKm,
      bedrooms: filters.bedrooms,
      beds: filters.beds,
      bathrooms: filters.bathrooms,
      propertyTypes: filters.propertyTypes.length > 0 ? filters.propertyTypes : null,
      amenityIds: filters.amenityIds.length > 0 ? filters.amenityIds : null,
      instantBook: filters.instantBook || null,
      swLat: filters.swLat,
      swLng: filters.swLng,
      neLat: filters.neLat,
      neLng: filters.neLng,
      sort: this.mapSort(filters.sort),
      page: filters.page,
      size: filters.size
    };

    Object.keys(body).forEach((key) => {
      if (body[key] === null || body[key] === undefined) {
        delete body[key];
      }
    });

    return body;
  }

  private mapSort(sort: SearchFilters['sort']): string {
    const mapping: Record<SearchFilters['sort'], string> = {
      relevance: 'RELEVANCE',
      priceAsc: 'PRICE_ASC',
      priceDesc: 'PRICE_DESC',
      newest: 'NEWEST',
      ratingDesc: 'RATING_DESC',
      distance: 'DISTANCE'
    };
    return mapping[sort];
  }
}

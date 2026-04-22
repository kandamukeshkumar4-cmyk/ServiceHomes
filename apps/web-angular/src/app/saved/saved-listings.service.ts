import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ListingSearchResult } from '../listings/listing.model';

@Injectable({ providedIn: 'root' })
export class SavedListingsService {
  private readonly http = inject(HttpClient);

  list(): Observable<ListingSearchResult[]> {
    return this.http.get<ListingSearchResult[]>('/api/saved-listings');
  }

  save(listingId: string): Observable<void> {
    return this.http.put<void>(`/api/saved-listings/${listingId}`, {});
  }

  unsave(listingId: string): Observable<void> {
    return this.http.delete<void>(`/api/saved-listings/${listingId}`);
  }
}

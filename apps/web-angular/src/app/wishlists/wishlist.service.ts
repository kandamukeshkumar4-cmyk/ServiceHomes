import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ListingSummary, SavedSearch, SaveSearchPayload, WishlistDetail, WishlistItem, WishlistSummary } from './wishlist.models';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);

  listWishlists(): Observable<WishlistSummary[]> {
    return this.http.get<WishlistSummary[]>('/api/wishlists');
  }

  wishlistIdsContainingListing(listingId: string): Observable<{ wishlistIds: string[] }> {
    return this.http.get<{ wishlistIds: string[] }>(`/api/wishlists/contains?listingId=${listingId}`);
  }

  createWishlist(payload: { title: string; description?: string; isPublic?: boolean }): Observable<WishlistSummary> {
    return this.http.post<WishlistSummary>('/api/wishlists', { ...payload, isPublic: Boolean(payload.isPublic) });
  }

  getWishlist(id: string, page = 0, size = 50): Observable<WishlistDetail> {
    return this.http.get<WishlistDetail>(`/api/wishlists/${id}?page=${page}&size=${size}`);
  }

  getSharedWishlist(token: string, page = 0, size = 50): Observable<WishlistDetail> {
    return this.http.get<WishlistDetail>(`/api/public/wishlists/share/${token}?page=${page}&size=${size}`);
  }

  addItem(wishlistId: string, listingId: string, note?: string, sourcePage = 'listing'): Observable<WishlistItem> {
    return this.http.post<WishlistItem>(`/api/wishlists/${wishlistId}/items`, { listingId, note, sourcePage });
  }

  updateItem(wishlistId: string, itemId: string, note: string): Observable<WishlistItem> {
    return this.http.patch<WishlistItem>(`/api/wishlists/${wishlistId}/items/${itemId}`, { note });
  }

  removeItem(wishlistId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`/api/wishlists/${wishlistId}/items/${itemId}`);
  }

  removeListing(wishlistId: string, listingId: string): Observable<void> {
    return this.http.delete<void>(`/api/wishlists/${wishlistId}/items/by-listing/${listingId}`);
  }

  reorderItems(wishlistId: string, itemIds: string[]): Observable<void> {
    return this.http.put<void>(`/api/wishlists/${wishlistId}/items/reorder`, { itemIds });
  }

  updateCollaborators(wishlistId: string, collaboratorIds: string[]): Observable<WishlistDetail> {
    return this.http.put<WishlistDetail>(`/api/wishlists/${wishlistId}/collaborators`, { collaboratorIds });
  }

  generateShareLink(wishlistId: string): Observable<{ token: string; url: string }> {
    return this.http.post<{ token: string; url: string }>(`/api/wishlists/${wishlistId}/share-link`, {});
  }

  updatePrivacy(wishlistId: string, isPublic: boolean): Observable<WishlistDetail> {
    return this.http.put<WishlistDetail>(`/api/wishlists/${wishlistId}/privacy?isPublic=${isPublic}`, {});
  }

  listSavedSearches(): Observable<SavedSearch[]> {
    return this.http.get<SavedSearch[]>('/api/saved-searches');
  }

  saveSearch(payload: SaveSearchPayload): Observable<SavedSearch> {
    return this.http.post<SavedSearch>('/api/saved-searches', payload);
  }

  deleteSavedSearch(id: string): Observable<void> {
    return this.http.delete<void>(`/api/saved-searches/${id}`);
  }

  recentlyViewed(): Observable<ListingSummary[]> {
    return this.http.get<ListingSummary[]>('/api/recently-viewed');
  }

  recordListingView(listingId: string, sourcePage = 'home'): Observable<void> {
    return this.http.post<void>('/api/recently-viewed', { listingId, sourcePage });
  }

  clearRecentlyViewed(): Observable<void> {
    return this.http.delete<void>('/api/recently-viewed');
  }
}

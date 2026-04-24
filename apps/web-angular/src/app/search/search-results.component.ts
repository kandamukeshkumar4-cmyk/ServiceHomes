import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, switchMap, tap } from 'rxjs';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { ListingCardComponent } from '../listings/listing-card.component';
import { ListingSearchResult } from '../listings/listing.model';
import { SavedListingsService } from '../saved/saved-listings.service';
import { AppAuthService } from '../core/auth.service';
import { SearchApiService, SearchApiResponse } from './search-api.service';
import { SearchBarComponent } from './search-bar.component';
import { DEFAULT_SEARCH_FILTERS, SearchFilters } from './search-filters.model';
import { SearchFiltersComponent } from './search-filters.component';
import { SearchMapBounds, SearchMapComponent } from './search-map.component';
import { SearchStateService } from './search-state.service';

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [CommonModule, SearchBarComponent, SearchFiltersComponent, ListingCardComponent, SearchMapComponent, PaginatorModule],
  templateUrl: './search-results.component.html',
  styleUrl: './search-results.component.scss'
})
export class SearchResultsComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchApiService = inject(SearchApiService);
  private readonly searchState = inject(SearchStateService);
  private readonly savedListingsService = inject(SavedListingsService);
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  readonly auth = inject(AppAuthService);
  private currentSearchQueryId: string | null = null;
  private savedSearchId: string | null = null;

  filters: SearchFilters = DEFAULT_SEARCH_FILTERS;
  results: ListingSearchResult[] = [];
  totalElements = 0;
  loading = false;
  error: string | null = null;
  viewMode: 'list' | 'map' = 'list';

  readonly chips = this.searchState.filters$;

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.savedSearchId = params.get('savedSearchId');
    });
  }

  constructor() {
    this.searchState.filters$
      .pipe(
        tap((filters) => {
          this.filters = filters;
          this.loading = true;
          this.error = null;
        }),
        switchMap((filters) => this.searchApiService.search(filters).pipe(
          finalize(() => {
            this.loading = false;
          }),
          catchError(() => {
            this.results = [];
            this.totalElements = 0;
            this.currentSearchQueryId = null;
            this.error = 'Search is unavailable right now. Please retry in a moment.';
            return of(null);
          })
        )),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((page: SearchApiResponse | null) => {
        if (!page) {
          return;
        }

        this.results = page.content;
        this.totalElements = page.totalElements;
        this.currentSearchQueryId = page.searchQueryId;

        if (this.savedSearchId) {
          this.http.post(`/api/saved-searches/${this.savedSearchId}/result-count`, { resultCount: this.totalElements }).subscribe();
        }
      });
  }

  get headerText(): string {
    if (!this.totalElements) {
      return this.filters.locationQuery
        ? `No stays found for "${this.filters.locationQuery}"`
        : 'No stays found';
    }

    return this.filters.locationQuery
      ? `${this.totalElements} stays for "${this.filters.locationQuery}"`
      : `${this.totalElements} stays`;
  }

  get activeChips(): Array<{ key: keyof SearchFilters | 'propertyType'; label: string; value?: string }> {
    const chips: Array<{ key: keyof SearchFilters | 'propertyType'; label: string; value?: string }> = [];
    if (this.filters.minPrice !== null || this.filters.maxPrice !== null) {
      chips.push({ key: 'minPrice', label: `Price ${this.filters.minPrice ?? 0}-${this.filters.maxPrice ?? 1000}` });
    }
    if (this.filters.bedrooms !== null) {
      chips.push({ key: 'bedrooms', label: `${this.filters.bedrooms}+ bedrooms` });
    }
    if (this.filters.guests > 1) {
      chips.push({ key: 'guests', label: `${this.filters.guests} guests` });
    }
    if (this.filters.checkIn || this.filters.checkOut) {
      chips.push({ key: 'checkIn', label: `${this.filters.checkIn ?? 'Any'} to ${this.filters.checkOut ?? 'Any'}` });
    }
    if (this.filters.instantBook) {
      chips.push({ key: 'instantBook', label: 'Instant Book' });
    }
    this.filters.propertyTypes.forEach((propertyType) => {
      chips.push({ key: 'propertyType', label: propertyType.replace(/_/g, ' '), value: propertyType });
    });
    return chips;
  }

  removeChip(chip: { key: keyof SearchFilters | 'propertyType'; value?: string }): void {
    if (chip.key === 'propertyType' && chip.value) {
      this.searchState.patchFilters({
        propertyTypes: this.filters.propertyTypes.filter((propertyType) => propertyType !== chip.value)
      });
      return;
    }

    if (chip.key === 'checkIn') {
      this.searchState.patchFilters({ checkIn: null, checkOut: null });
      return;
    }

    this.searchState.removeFilter(chip.key as keyof SearchFilters);
  }

  handlePageChange(event: PaginatorState): void {
    this.searchState.patchFilters({
      page: event.page ?? 0,
      size: event.rows ?? this.filters.size
    }, false);
  }

  setViewMode(viewMode: 'list' | 'map'): void {
    this.viewMode = viewMode;
  }

  handleListingClick(listingId: string, position: number): void {
    if (!this.currentSearchQueryId) {
      return;
    }
    const globalPosition = this.filters.page * this.filters.size + position + 1;
    this.searchApiService.recordClick(this.currentSearchQueryId, listingId, globalPosition).subscribe({
      error: () => { /* silently fail; analytics are best-effort */ }
    });
  }

  handleSaveToggle(listingId: string): void {
    const listing = this.results.find((entry) => entry.id === listingId);
    if (!listing) {
      return;
    }

    const previous = listing.isSaved === true;
    this.results = this.results.map((entry) =>
      entry.id === listingId ? { ...entry, isSaved: !previous } : entry
    );

    const request = previous
      ? this.savedListingsService.unsave(listingId)
      : this.savedListingsService.save(listingId);

    request.subscribe({
      error: () => {
        this.results = this.results.map((entry) =>
          entry.id === listingId ? { ...entry, isSaved: previous } : entry
        );
      }
    });
  }

  handleSearchAreaRequested(bounds: SearchMapBounds): void {
    this.searchState.patchFilters({
      swLat: bounds.swLat,
      swLng: bounds.swLng,
      neLat: bounds.neLat,
      neLng: bounds.neLng
    });
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CategoryShellComponent } from '../shell/category-shell.component';
import { SearchBarComponent } from '../search/search-bar.component';
import { ListingCardComponent } from '../listings/listing-card.component';
import { ListingCardViewModel, ListingSearchPage } from '../listings/listing.model';
import { SavedListingsService } from '../saved/saved-listings.service';
import { AppAuthService } from '../core/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CategoryShellComponent, SearchBarComponent, ListingCardComponent],
  templateUrl: './home.component.html',
  styles: []
})
export class HomeComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private savedListingsService = inject(SavedListingsService);
  auth = inject(AppAuthService);

  listings: ListingCardViewModel[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.loadListings(params);
    });
  }

  loadListings(params: Record<string, any>) {
    this.loading = true;
    this.error = null;
    let httpParams = new HttpParams();
    if (params['locationQuery']) httpParams = httpParams.set('locationQuery', params['locationQuery']);
    if (params['categoryId']) httpParams = httpParams.set('categoryId', params['categoryId']);
    if (params['checkIn']) httpParams = httpParams.set('checkIn', params['checkIn']);
    if (params['checkOut']) httpParams = httpParams.set('checkOut', params['checkOut']);
    if (params['guests']) httpParams = httpParams.set('guests', params['guests']);

    httpParams = httpParams.set('page', '0').set('size', '8').set('sort', 'NEWEST');

    this.http.get<ListingSearchPage>('/api/listings/search', { params: httpParams }).subscribe({
      next: data => {
        this.listings = data.content;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load listings. Please try again.';
        this.listings = [];
        this.loading = false;
      }
    });
  }

  handleSaveToggle(listingId: string): void {
    const listing = this.listings.find((entry) => entry.id === listingId);
    if (!listing) {
      return;
    }

    const previous = listing.isSaved === true;
    this.listings = this.listings.map((entry) =>
      entry.id === listingId ? { ...entry, isSaved: !previous } : entry
    );

    const request = previous
      ? this.savedListingsService.unsave(listingId)
      : this.savedListingsService.save(listingId);

    request.subscribe({
      error: () => {
        this.listings = this.listings.map((entry) =>
          entry.id === listingId ? { ...entry, isSaved: previous } : entry
        );
      }
    });
  }
}

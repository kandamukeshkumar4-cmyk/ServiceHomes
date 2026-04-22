import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ListingCardComponent } from '../listings/listing-card.component';
import { ListingSearchResult } from '../listings/listing.model';
import { SavedListingsService } from './saved-listings.service';

@Component({
  selector: 'app-saved-listings-page',
  standalone: true,
  imports: [CommonModule, ListingCardComponent],
  templateUrl: './saved-listings-page.component.html',
  styles: []
})
export class SavedListingsPageComponent implements OnInit {
  private readonly savedListingsService = inject(SavedListingsService);

  loading = true;
  errorMessage: string | null = null;
  listings: ListingSearchResult[] = [];

  ngOnInit(): void {
    this.loadSavedListings();
  }

  handleSaveToggle(listingId: string): void {
    const existing = this.listings.find((listing) => listing.id === listingId);
    if (!existing) {
      return;
    }

    const previousListings = this.listings;
    this.listings = this.listings.filter((listing) => listing.id !== listingId);

    this.savedListingsService.unsave(listingId).subscribe({
      error: (error: unknown) => {
        this.listings = previousListings;
        this.errorMessage = this.describeError(error, 'Unable to update your saved listings right now.');
      }
    });
  }

  private loadSavedListings(): void {
    this.loading = true;
    this.errorMessage = null;

    this.savedListingsService.list().subscribe({
      next: (listings) => {
        this.listings = listings;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error, 'Unable to load your saved listings right now.');
        this.loading = false;
      }
    });
  }

  private describeError(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string' && error.error.trim().length > 0) {
        return error.error;
      }

      const message = error.error?.message;
      if (typeof message === 'string' && message.trim().length > 0) {
        return message;
      }
    }

    return fallback;
  }
}

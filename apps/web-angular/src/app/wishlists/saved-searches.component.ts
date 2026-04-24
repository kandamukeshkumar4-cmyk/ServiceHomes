import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SavedSearch } from './wishlist.models';
import { WishlistService } from './wishlist.service';

@Component({
  selector: 'app-saved-searches',
  standalone: true,
  imports: [CommonModule, FormsModule, BadgeModule, ButtonModule, ChipModule, InputTextModule, InputTextareaModule],
  template: `
    <section class="saved-searches">
      <h1>Saved Searches</h1>
      <form class="saved-searches__create" (ngSubmit)="createSearch()">
        <input pInputText name="name" [(ngModel)]="newSearchName" placeholder="Name" />
        <input pInputText name="location" [(ngModel)]="newLocationQuery" placeholder="Location" />
        <textarea pInputTextarea name="filters" [(ngModel)]="newFiltersJson" rows="3"></textarea>
        <label>
          <input type="checkbox" name="notify" [(ngModel)]="notifyNewResults" />
          <span>Alerts</span>
        </label>
        <button pButton type="submit" icon="pi pi-bookmark" label="Save search"></button>
        <p class="saved-searches__error" *ngIf="createError">{{ createError }}</p>
      </form>
      <article class="saved-search" *ngFor="let search of searches">
        <div>
          <h2>{{ search.name }}</h2>
          <p>{{ search.locationQuery || 'Any location' }}</p>
          <div class="saved-search__chips">
            <p-chip *ngFor="let chip of filterChips(search)" [label]="chip"></p-chip>
          </div>
        </div>
        <div class="saved-search__actions">
          <span pBadge [value]="search.resultCountSnapshot ?? 0"></span>
          <button pButton type="button" icon="pi pi-search" label="Run search" (click)="runSearch(search)"></button>
          <button pButton type="button" icon="pi pi-trash" severity="danger" (click)="deleteSearch(search)"></button>
        </div>
      </article>
    </section>
  `,
  styles: [`
    .saved-searches { padding: 2rem; }
    .saved-searches__create { align-items: flex-start; border: 1px solid #e2e8f0; border-radius: 8px; display: grid; gap: .75rem; grid-template-columns: minmax(10rem, 1fr) minmax(10rem, 1fr) minmax(14rem, 2fr) auto auto; margin-bottom: 1rem; padding: 1rem; }
    .saved-searches__create label { align-items: center; display: flex; gap: .4rem; padding-top: .55rem; }
    .saved-searches__create textarea { resize: vertical; }
    .saved-searches__error { color: #b91c1c; grid-column: 1 / -1; margin: 0; }
    .saved-search { align-items: center; border: 1px solid #e2e8f0; border-radius: 8px; display: flex; gap: 1rem; justify-content: space-between; margin-bottom: 1rem; padding: 1rem; }
    .saved-search h2 { font-size: 1.1rem; margin: 0; }
    .saved-search__chips, .saved-search__actions { align-items: center; display: flex; flex-wrap: wrap; gap: .5rem; }
  `]
})
export class SavedSearchesComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly wishlistService = inject(WishlistService);

  searches: SavedSearch[] = [];
  newSearchName = '';
  newLocationQuery = '';
  newFiltersJson = '{}';
  notifyNewResults = false;
  createError = '';

  ngOnInit(): void {
    this.wishlistService.listSavedSearches().subscribe((searches) => this.searches = searches);
  }

  filterChips(search: SavedSearch): string[] {
    return Object.entries(search.filters ?? {}).map(([key, value]) => `${key}: ${value}`);
  }

  runSearch(search: SavedSearch): void {
    const queryParams: Record<string, unknown> = { ...search.filters, savedSearchId: search.id };
    if (search.locationQuery) {
      queryParams['locationQuery'] = search.locationQuery;
    }
    if (search.geoCenterLat != null) {
      queryParams['lat'] = search.geoCenterLat;
    }
    if (search.geoCenterLng != null) {
      queryParams['lng'] = search.geoCenterLng;
    }
    if (search.radiusKm != null) {
      queryParams['radiusKm'] = search.radiusKm;
    }
    this.router.navigate(['/search'], { queryParams });
  }

  createSearch(): void {
    const name = this.newSearchName.trim();
    const filters = this.parseFilters();
    if (!name || !filters) {
      return;
    }
    const locationQuery = this.newLocationQuery.trim();
    this.wishlistService.saveSearch({
      name,
      filters,
      locationQuery: locationQuery || null,
      notifyNewResults: this.notifyNewResults
    }).subscribe((savedSearch) => {
      this.searches = [savedSearch, ...this.searches];
      this.newSearchName = '';
      this.newLocationQuery = '';
      this.newFiltersJson = '{}';
      this.notifyNewResults = false;
      this.createError = '';
    });
  }

  deleteSearch(search: SavedSearch): void {
    this.searches = this.searches.filter((existing) => existing.id !== search.id);
    this.wishlistService.deleteSavedSearch(search.id).subscribe();
  }

  private parseFilters(): Record<string, unknown> | null {
    try {
      const parsed = JSON.parse(this.newFiltersJson || '{}');
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        this.createError = '';
        return parsed as Record<string, unknown>;
      }
    } catch {
      // fall through to shared validation message
    }
    this.createError = 'Enter a valid filter object.';
    return null;
  }
}

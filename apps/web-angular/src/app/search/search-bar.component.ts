import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { SearchStateService } from './search-state.service';


@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="surface-0 shadow-1 border-round-xl p-3 flex flex-wrap gap-2 align-items-end">
      <div class="flex flex-column gap-1 flex-1 min-w-10rem">
        <label class="text-xs font-bold text-700">Where</label>
        <input class="p-inputtext" [(ngModel)]="location" placeholder="Search destinations" />
      </div>
      <div class="flex flex-column gap-1 flex-1 min-w-10rem">
        <label class="text-xs font-bold text-700">Check in</label>
        <input type="date" class="p-inputtext" [(ngModel)]="checkIn" />
      </div>
      <div class="flex flex-column gap-1 flex-1 min-w-10rem">
        <label class="text-xs font-bold text-700">Check out</label>
        <input type="date" class="p-inputtext" [(ngModel)]="checkOut" />
      </div>
      <div class="flex flex-column gap-1 flex-1 min-w-10rem">
        <label class="text-xs font-bold text-700">Guests</label>
        <input type="number" class="p-inputtext" [(ngModel)]="guests" min="1" />
      </div>
      <button class="p-button p-button-primary" (click)="search()">
        <i class="pi pi-search mr-1"></i> Search
      </button>
    </div>
  `,
  styles: []
})
export class SearchBarComponent {
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private searchState = inject(SearchStateService);

  location = '';
  checkIn = '';
  checkOut = '';
  guests = 1;

  constructor() {
    this.searchState.filters$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((filters) => {
        this.location = filters.locationQuery;
        this.checkIn = filters.checkIn ?? '';
        this.checkOut = filters.checkOut ?? '';
        this.guests = filters.guests;
      });
  }

  search() {
    const filters = {
      ...this.searchState.snapshot,
      locationQuery: this.location.trim(),
      checkIn: this.checkIn || null,
      checkOut: this.checkOut || null,
      guests: Math.max(1, this.guests),
      page: 0
    };
    this.router.navigate(['/search'], { queryParams: this.searchState.toQueryParams(filters) });
  }
}

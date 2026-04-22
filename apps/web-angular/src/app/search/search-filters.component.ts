import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SearchFilters } from './search-filters.model';
import { SearchStateService } from './search-state.service';

@Component({
  selector: 'app-search-filters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-filters.component.html',
  styles: [`
    .filter-panel {
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      border: 1px solid rgba(148, 163, 184, 0.22);
      position: sticky;
      top: 5.5rem;
    }

    .stepper-button {
      border: 1px solid rgba(148, 163, 184, 0.35);
      border-radius: 999px;
      height: 2rem;
      width: 2rem;
    }
  `]
})
export class SearchFiltersComponent {
  @Input({ required: true }) filters!: SearchFilters;

  readonly propertyTypeOptions = [
    'Apartment',
    'House',
    'Villa',
    'Cabin',
    'Cottage',
    'Tiny Home',
    'Treehouse',
    'Boat',
    'Camper'
  ];

  constructor(private readonly searchState: SearchStateService) {}

  updateMinPrice(value: string | number): void {
    this.searchState.patchFilters({ minPrice: value === '' ? null : Number(value) });
  }

  updateMaxPrice(value: string | number): void {
    this.searchState.patchFilters({ maxPrice: value === '' ? null : Number(value) });
  }

  updateBedrooms(delta: number): void {
    const next = Math.max(1, (this.filters.bedrooms ?? 1) + delta);
    this.searchState.patchFilters({ bedrooms: next });
  }

  clearBedrooms(): void {
    this.searchState.patchFilters({ bedrooms: null });
  }

  updateGuests(delta: number): void {
    const next = Math.max(1, this.filters.guests + delta);
    this.searchState.patchFilters({ guests: next });
  }

  updateDate(field: 'checkIn' | 'checkOut', value: string): void {
    this.searchState.patchFilters({ [field]: value || null } as Partial<SearchFilters>);
  }

  togglePropertyType(option: string, enabled: boolean): void {
    const normalized = option.toUpperCase().replace(/\s+/g, '_');
    const next = enabled
      ? [...this.filters.propertyTypes, normalized]
      : this.filters.propertyTypes.filter((propertyType) => propertyType !== normalized);
    this.searchState.patchFilters({ propertyTypes: next });
  }

  updateInstantBook(enabled: boolean): void {
    this.searchState.patchFilters({ instantBook: enabled });
  }

  updateSort(value: SearchFilters['sort']): void {
    this.searchState.patchFilters({ sort: value }, false);
  }

  clearAll(): void {
    this.searchState.clearFilters();
  }

  isChecked(option: string): boolean {
    return this.filters.propertyTypes.includes(option.toUpperCase().replace(/\s+/g, '_'));
  }
}

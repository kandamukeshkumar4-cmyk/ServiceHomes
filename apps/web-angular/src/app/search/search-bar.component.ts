import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ListingService } from '../listings/listing.service';
import { ListingCardDto } from '../listings/listing.model';

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
  location = '';
  checkIn = '';
  checkOut = '';
  guests = 1;

  search() {
    // Emit search event or navigate with query params
    // For now this is a shell component
  }
}

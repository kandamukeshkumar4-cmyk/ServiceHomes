import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CategoryShellComponent } from '../shell/category-shell.component';
import { SearchBarComponent } from '../search/search-bar.component';
import { ListingCardDto } from '../listings/listing.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, CategoryShellComponent, SearchBarComponent],
  template: `
    <app-category-shell />
    <div class="p-4 max-w-80rem mx-auto">
      <app-search-bar class="block mb-4" />

      <div *ngIf="listings.length === 0" class="text-center p-4">
        <p class="text-600">No listings available yet.</p>
      </div>

      <div class="grid gap-3">
        <div *ngFor="let listing of listings" class="col-12 sm:col-6 md:col-4 lg:col-3">
          <a [routerLink]="['/listings', listing.id]" class="no-underline text-900">
            <div class="border-round-xl overflow-hidden shadow-1 hover:shadow-3 transition-all cursor-pointer">
              <div class="h-12rem surface-200 relative">
                <img *ngIf="listing.coverUrl" [src]="listing.coverUrl" class="w-full h-full object-cover" alt="cover" />
                <span *ngIf="!listing.coverUrl" class="absolute inset-0 flex align-items-center justify-content-center text-500">No image</span>
              </div>
              <div class="p-2">
                <div class="flex justify-content-between align-items-start">
                  <h3 class="text-md font-bold m-0">{{ listing.city }}, {{ listing.country }}</h3>
                  <span class="text-sm">★ New</span>
                </div>
                <p class="text-600 text-sm m-0 mt-1">{{ listing.title }}</p>
                <p class="text-600 text-sm m-0">{{ listing.bedrooms }} beds · {{ listing.maxGuests }} guests</p>
                <p class="text-900 font-bold m-0 mt-1">${{ listing.nightlyPrice }} <span class="text-600 font-normal">night</span></p>
              </div>
            </div>
          </a>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class HomeComponent implements OnInit {
  private http = inject(HttpClient);
  listings: ListingCardDto[] = [];

  ngOnInit() {
    this.http.get<ListingCardDto[]>('/api/listings/search').subscribe({
      next: data => this.listings = data,
      error: () => this.listings = []
    });
  }
}

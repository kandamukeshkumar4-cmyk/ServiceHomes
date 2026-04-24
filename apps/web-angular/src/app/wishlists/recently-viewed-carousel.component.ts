import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ListingSummary } from './wishlist.models';
import { WishlistService } from './wishlist.service';

@Component({
  selector: 'app-recently-viewed-carousel',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonModule],
  template: `
    <section class="recently-viewed" *ngIf="listings.length">
      <header>
        <h2>Recently viewed</h2>
        <button pButton type="button" icon="pi pi-times" label="Clear" (click)="clearHistory()"></button>
      </header>
      <div class="recently-viewed__track">
        <a class="recently-viewed__card" *ngFor="let listing of listings | slice:0:8" [routerLink]="['/listings', listing.id]" [queryParams]="{ sourcePage: 'home' }">
          <img *ngIf="listing.thumbnail" [src]="listing.thumbnail" [alt]="listing.title" />
          <strong>{{ listing.title }}</strong>
          <span>{{ listing.price | currency }} night</span>
        </a>
      </div>
    </section>
  `,
  styles: [`
    .recently-viewed { padding: 1rem 0; }
    header { align-items: center; display: flex; justify-content: space-between; }
    .recently-viewed__track { display: flex; gap: .75rem; overflow-x: auto; padding-bottom: .5rem; }
    .recently-viewed__card { border: 1px solid #e2e8f0; border-radius: 8px; color: inherit; flex: 0 0 13rem; overflow: hidden; text-decoration: none; }
    img { aspect-ratio: 4 / 3; object-fit: cover; width: 100%; }
    strong, span { display: block; margin: .5rem .75rem; }
  `]
})
export class RecentlyViewedCarouselComponent implements OnInit {
  private readonly wishlistService = inject(WishlistService);

  listings: ListingSummary[] = [];

  ngOnInit(): void {
    this.wishlistService.recentlyViewed().subscribe((listings) => this.listings = listings);
  }

  clearHistory(): void {
    this.listings = [];
    this.wishlistService.clearRecentlyViewed().subscribe();
  }
}

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { WishlistDetail } from './wishlist.models';
import { WishlistService } from './wishlist.service';

@Component({
  selector: 'app-public-wishlist',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="public-wishlist" *ngIf="wishlist">
      <h1>{{ wishlist.title }}</h1>
      <div class="public-wishlist__grid">
        <article *ngFor="let item of wishlist.items">
          <img *ngIf="item.listing.thumbnail" [src]="item.listing.thumbnail" [alt]="item.listing.title" />
          <h2>{{ item.listing.title }}</h2>
          <p>{{ item.listing.price | currency }} night</p>
        </article>
      </div>
      <button type="button" class="p-button mt-3" *ngIf="hasMoreItems" (click)="loadMoreItems()">Load more</button>
    </section>
  `,
  styles: [`
    .public-wishlist { padding: 2rem; }
    .public-wishlist__grid { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr)); }
    article { border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
    img { aspect-ratio: 4 / 3; object-fit: cover; width: 100%; }
    h2, p { margin: .75rem; }
  `]
})
export class PublicWishlistComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly wishlistService = inject(WishlistService);

  wishlist: WishlistDetail | null = null;
  private token = '';
  private page = 0;
  private readonly pageSize = 50;

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    if (this.token) {
      this.wishlistService.getSharedWishlist(this.token, this.page, this.pageSize).subscribe((wishlist) => this.wishlist = wishlist);
    }
  }

  get hasMoreItems(): boolean {
    return !!this.wishlist && this.wishlist.items.length < this.wishlist.totalItems;
  }

  loadMoreItems(): void {
    if (!this.wishlist || !this.hasMoreItems) {
      return;
    }
    this.page += 1;
    this.wishlistService.getSharedWishlist(this.token, this.page, this.pageSize).subscribe((nextPage) => {
      if (this.wishlist) {
        this.wishlist = { ...nextPage, items: [...this.wishlist.items, ...nextPage.items] };
      }
    });
  }
}

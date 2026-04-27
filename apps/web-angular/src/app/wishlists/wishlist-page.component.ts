import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { WishlistSummary } from './wishlist.models';
import { WishlistService } from './wishlist.service';

@Component({
  selector: 'app-wishlist-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ButtonModule, CardModule, InputTextModule, TagModule],
  template: `
    <section class="wishlist-page">
      <header class="wishlist-page__header">
        <h1>Wishlists</h1>
        <form class="wishlist-page__create" (ngSubmit)="createWishlist()">
          <input pInputText name="title" [(ngModel)]="newTitle" placeholder="New wishlist" required />
          <button pButton type="submit" icon="pi pi-plus" label="Create"></button>
        </form>
      </header>

      <p class="wishlist-page__empty" *ngIf="!loading && wishlists.length === 0">No wishlists yet.</p>

      <div class="wishlist-grid">
        <p-card *ngFor="let wishlist of wishlists" styleClass="wishlist-card">
          <ng-template pTemplate="header">
            <a [routerLink]="['/wishlists', wishlist.id]" class="wishlist-card__media">
              <img *ngIf="wishlist.coverPhotoUrl; else fallback" [src]="wishlist.coverPhotoUrl" [alt]="wishlist.title" />
              <ng-template #fallback><span class="pi pi-heart"></span></ng-template>
            </a>
          </ng-template>
          <div class="wishlist-card__body">
            <div>
              <a [routerLink]="['/wishlists', wishlist.id]"><h2>{{ wishlist.title }}</h2></a>
              <p>{{ wishlist.itemCount }} stays</p>
            </div>
            <p-tag [value]="wishlist.isPublic ? 'Public' : 'Private'"></p-tag>
          </div>
        </p-card>
      </div>
    </section>
  `,
  styles: [`
    .wishlist-page { padding: 2rem; }
    .wishlist-page__header { align-items: center; display: flex; gap: 1rem; justify-content: space-between; }
    .wishlist-page__create { display: flex; gap: .5rem; }
    .wishlist-grid { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); }
    .wishlist-card__media { align-items: center; aspect-ratio: 16 / 10; background: #eef2f7; color: #64748b; display: flex; justify-content: center; overflow: hidden; }
    .wishlist-card__media img { height: 100%; object-fit: cover; width: 100%; }
    .wishlist-card__body { align-items: flex-start; display: flex; justify-content: space-between; gap: 1rem; }
    h1 { margin: 0 0 1rem; }
    h2 { font-size: 1.1rem; margin: 0; }
  `]
})
export class WishlistPageComponent implements OnInit {
  private readonly wishlistService = inject(WishlistService);

  loading = true;
  newTitle = '';
  wishlists: WishlistSummary[] = [];

  ngOnInit(): void {
    this.wishlistService.listWishlists().subscribe({
      next: (wishlists) => {
        this.wishlists = wishlists;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  createWishlist(): void {
    const title = this.newTitle.trim();
    if (!title) {
      return;
    }
    this.wishlistService.createWishlist({ title }).subscribe((wishlist) => {
      this.wishlists = [wishlist, ...this.wishlists];
      this.newTitle = '';
    });
  }
}

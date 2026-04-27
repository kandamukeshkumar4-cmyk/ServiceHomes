import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { WishlistSummary } from './wishlist.models';
import { WishlistService } from './wishlist.service';

@Component({
  selector: 'app-save-to-wishlist',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, CheckboxModule, InputTextModule, OverlayPanelModule],
  template: `
    <button pButton type="button" icon="pi pi-heart" class="save-button" (click)="panel.toggle($event)"></button>
    <p-overlayPanel #panel>
      <div class="save-panel">
        <label *ngFor="let wishlist of wishlists">
          <p-checkbox [binary]="true" [ngModel]="savedWishlistIds.has(wishlist.id)" (onChange)="toggleWishlist(wishlist)"></p-checkbox>
          <span>{{ wishlist.title }}</span>
        </label>
        <form class="save-panel__create" (ngSubmit)="createWishlist()">
          <input pInputText name="title" [(ngModel)]="newTitle" placeholder="Create wishlist" />
          <button pButton type="submit" icon="pi pi-plus"></button>
        </form>
      </div>
    </p-overlayPanel>
  `,
  styles: [`
    .save-button { border-radius: 999px; height: 2.5rem; width: 2.5rem; }
    .save-panel { display: grid; gap: .75rem; min-width: 16rem; }
    .save-panel label, .save-panel__create { align-items: center; display: flex; gap: .5rem; }
  `]
})
export class SaveToWishlistComponent implements OnInit {
  @Input({ required: true }) listingId!: string;
  @ViewChild('panel') panel?: OverlayPanel;

  private readonly wishlistService = inject(WishlistService);

  wishlists: WishlistSummary[] = [];
  savedWishlistIds = new Set<string>();
  newTitle = '';

  ngOnInit(): void {
    forkJoin({
      wishlists: this.wishlistService.listWishlists(),
      saved: this.wishlistService.wishlistIdsContainingListing(this.listingId)
    }).subscribe(({ wishlists, saved }) => {
      this.wishlists = wishlists;
      this.savedWishlistIds = new Set(saved.wishlistIds);
    });
  }

  toggleWishlist(wishlist: WishlistSummary): void {
    if (this.savedWishlistIds.has(wishlist.id)) {
      this.wishlistService.removeListing(wishlist.id, this.listingId).subscribe(() => this.savedWishlistIds.delete(wishlist.id));
      return;
    }
    this.wishlistService.addItem(wishlist.id, this.listingId).subscribe(() => this.savedWishlistIds.add(wishlist.id));
  }

  createWishlist(): void {
    const title = this.newTitle.trim();
    if (!title) {
      return;
    }
    this.wishlistService.createWishlist({ title }).subscribe((wishlist) => {
      this.wishlists = [wishlist, ...this.wishlists];
      this.newTitle = '';
      this.toggleWishlist(wishlist);
    });
  }
}

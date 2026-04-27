import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { WishlistDetail, WishlistItem } from './wishlist.models';
import { WishlistService } from './wishlist.service';

@Component({
  selector: 'app-wishlist-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, DialogModule, InputSwitchModule, InputTextModule, InputTextareaModule],
  template: `
    <section class="wishlist-detail" *ngIf="wishlist">
      <header class="wishlist-detail__header">
        <div>
          <h1>{{ wishlist.title }}</h1>
          <p>{{ wishlist.description }}</p>
        </div>
        <div class="wishlist-detail__actions" *ngIf="wishlist.owner">
          <p-inputSwitch [(ngModel)]="wishlist.isPublic" (onChange)="togglePrivacy()"></p-inputSwitch>
          <button pButton type="button" icon="pi pi-users" label="Collaborators" (click)="collaboratorDialogOpen = true"></button>
          <button pButton type="button" icon="pi pi-link" label="Share" (click)="generateShareLink()"></button>
        </div>
      </header>

      <div class="share-link" *ngIf="shareLink">
        <input pInputText [value]="shareLink" readonly />
        <button pButton type="button" icon="pi pi-copy" (click)="copyShareLink()"></button>
      </div>

      <div class="wishlist-items">
        <article class="wishlist-item" *ngFor="let item of wishlist.items; let i = index">
          <img *ngIf="item.listing.thumbnail" [src]="item.listing.thumbnail" [alt]="item.listing.title" />
          <div class="wishlist-item__content">
            <h2>{{ item.listing.title }}</h2>
            <p>{{ item.listing.price | currency }} night <span *ngIf="item.listing.rating">· {{ item.listing.rating }} rating</span></p>
            <textarea pInputTextarea *ngIf="wishlist.editable" [(ngModel)]="item.note" (blur)="saveNote(item)" rows="2"></textarea>
            <p *ngIf="!wishlist.editable">{{ item.note }}</p>
          </div>
          <div class="wishlist-item__tools" *ngIf="wishlist.editable">
            <button pButton type="button" icon="pi pi-arrow-up" [disabled]="i === 0" (click)="moveItem(i, -1)"></button>
            <button pButton type="button" icon="pi pi-arrow-down" [disabled]="i === wishlist.items.length - 1" (click)="moveItem(i, 1)"></button>
            <button pButton type="button" icon="pi pi-trash" severity="danger" *ngIf="wishlist.editable" (click)="removeItem(item)"></button>
          </div>
        </article>
      </div>

      <button pButton type="button" label="Load more" icon="pi pi-angle-down" *ngIf="hasMoreItems" (click)="loadMoreItems()"></button>

      <p-dialog header="Collaborators" [(visible)]="collaboratorDialogOpen" [modal]="true" *ngIf="wishlist.owner">
        <label for="collaborators">Guest IDs</label>
        <textarea id="collaborators" pInputTextarea rows="5" [(ngModel)]="collaboratorText"></textarea>
        <ng-template pTemplate="footer">
          <button pButton type="button" label="Save" icon="pi pi-check" (click)="saveCollaborators()"></button>
        </ng-template>
      </p-dialog>
    </section>
  `,
  styles: [`
    .wishlist-detail { padding: 2rem; }
    .wishlist-detail__header, .wishlist-detail__actions, .share-link { align-items: center; display: flex; gap: 1rem; justify-content: space-between; }
    .share-link { justify-content: flex-start; margin: 1rem 0; }
    .wishlist-items { display: grid; gap: 1rem; }
    .wishlist-item { border: 1px solid #e2e8f0; border-radius: 8px; display: grid; gap: 1rem; grid-template-columns: 10rem 1fr auto; padding: 1rem; }
    .wishlist-item img { aspect-ratio: 4 / 3; border-radius: 6px; height: auto; object-fit: cover; width: 10rem; }
    .wishlist-item__content h2 { font-size: 1rem; margin: 0; }
    .wishlist-item__tools { display: flex; gap: .5rem; }
    textarea { width: 100%; }
  `]
})
export class WishlistDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly wishlistService = inject(WishlistService);

  wishlist: WishlistDetail | null = null;
  collaboratorDialogOpen = false;
  collaboratorText = '';
  shareLink = '';
  private wishlistId = '';
  private page = 0;
  private readonly pageSize = 50;

  ngOnInit(): void {
    this.wishlistId = this.route.snapshot.paramMap.get('id') ?? '';
    if (this.wishlistId) {
      this.loadWishlist();
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
    this.wishlistService.getWishlist(this.wishlist.id, this.page, this.pageSize).subscribe((nextPage) => {
      if (this.wishlist) {
        this.wishlist = { ...nextPage, items: [...this.wishlist.items, ...nextPage.items] };
      }
    });
  }

  saveNote(item: WishlistItem): void {
    if (this.wishlist) {
      this.wishlistService.updateItem(this.wishlist.id, item.id, item.note ?? '').subscribe();
    }
  }

  moveItem(index: number, delta: number): void {
    if (!this.wishlist) {
      return;
    }
    if (this.hasMoreItems) {
      this.loadAllItemsAndThen(() => this.doMoveItem(index, delta));
      return;
    }
    this.doMoveItem(index, delta);
  }

  private doMoveItem(index: number, delta: number): void {
    if (!this.wishlist) {
      return;
    }
    const next = [...this.wishlist.items];
    const [item] = next.splice(index, 1);
    next.splice(index + delta, 0, item);
    this.wishlist.items = next;
    this.wishlistService.reorderItems(this.wishlist.id, next.map((wishlistItem) => wishlistItem.id)).subscribe();
  }

  private loadAllItemsAndThen(then: () => void): void {
    if (!this.wishlist) {
      return;
    }
    const wishlistId = this.wishlist.id;
    const total = this.wishlist.totalItems;
    const pageSize = 50;
    const pagesNeeded = Math.ceil(total / pageSize);
    let allItems: WishlistItem[] = [];
    let loaded = 0;

    const loadPage = (page: number) => {
      this.wishlistService.getWishlist(wishlistId, page, pageSize).subscribe((result) => {
        allItems = allItems.concat(result.items);
        loaded++;
        if (loaded >= pagesNeeded) {
          if (this.wishlist) {
            this.wishlist = { ...this.wishlist, items: allItems };
            this.page = 0;
          }
          then();
        } else {
          loadPage(page + 1);
        }
      });
    };
    loadPage(0);
  }

  removeItem(item: WishlistItem): void {
    if (!this.wishlist) {
      return;
    }
    const wishlistId = this.wishlist.id;
    this.wishlistService.removeItem(wishlistId, item.id).subscribe(() => this.loadWishlist());
  }

  saveCollaborators(): void {
    if (!this.wishlist) {
      return;
    }
    const ids = this.collaboratorText.split(/\s|,/).map((value) => value.trim()).filter(Boolean);
    this.wishlistService.updateCollaborators(this.wishlist.id, ids).subscribe((wishlist) => {
      this.wishlist = wishlist;
      this.collaboratorDialogOpen = false;
    });
  }

  generateShareLink(): void {
    if (!this.wishlist) {
      return;
    }
    this.wishlistService.generateShareLink(this.wishlist.id).subscribe((share) => {
      this.shareLink = `${location.origin}${share.url}`;
      if (this.wishlist) {
        this.wishlist = { ...this.wishlist, isPublic: true, shareToken: share.token };
      }
    });
  }

  copyShareLink(): void {
    if (this.shareLink) {
      navigator.clipboard?.writeText(this.shareLink);
    }
  }

  togglePrivacy(): void {
    if (this.wishlist) {
      this.wishlistService.updatePrivacy(this.wishlist.id, this.wishlist.isPublic).subscribe((wishlist) => this.wishlist = wishlist);
    }
  }

  private loadWishlist(): void {
    this.page = 0;
    this.wishlistService.getWishlist(this.wishlistId, this.page, this.pageSize).subscribe((wishlist) => {
      this.wishlist = wishlist;
      this.collaboratorText = wishlist.collaboratorIds.join('\n');
    });
  }
}

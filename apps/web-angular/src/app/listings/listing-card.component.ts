import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ListingCardViewModel } from './listing.model';

@Component({
  selector: 'app-listing-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './listing-card.component.html',
  styles: [`
    .listing-card {
      background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
      border: 1px solid rgba(15, 23, 42, 0.08);
      position: relative;
    }

    .listing-card__media {
      position: relative;
      height: 13rem;
      background: linear-gradient(135deg, #dbeafe 0%, #f8fafc 100%);
    }

    .listing-card__media-link,
    .listing-card__fallback {
      display: block;
      height: 100%;
      width: 100%;
    }

    .listing-card__fallback {
      align-items: center;
      color: #64748b;
      display: flex;
      justify-content: center;
    }

    .listing-card__image {
      height: 100%;
      object-fit: cover;
      width: 100%;
    }

    .listing-card__save {
      align-items: center;
      border: 0;
      border-radius: 999px;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
      cursor: pointer;
      display: inline-flex;
      height: 2.5rem;
      justify-content: center;
      position: absolute;
      right: 0.9rem;
      top: 0.9rem;
      width: 2.5rem;
    }

    .listing-card__title-link:hover h3 {
      text-decoration: underline;
    }
  `]
})
export class ListingCardComponent {
  @Input({ required: true }) listing!: ListingCardViewModel;
  @Input() showSaveToggle = false;
  @Output() readonly saveToggled = new EventEmitter<string>();
  @Output() readonly listingOpened = new EventEmitter<string>();

  openListing(): void {
    this.listingOpened.emit(this.listing.id);
  }

  toggleSaved(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.saveToggled.emit(this.listing.id);
  }
}

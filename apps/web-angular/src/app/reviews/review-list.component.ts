import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HostResponseFormComponent } from './host-response-form.component';
import { ListingReview } from './reviews-api.service';

@Component({
  selector: 'app-review-list',
  standalone: true,
  imports: [CommonModule, HostResponseFormComponent],
  templateUrl: './review-list.component.html',
  styles: [`
    .breakdown-track {
      height: 0.5rem;
      background: var(--surface-200, #e5e7eb);
      overflow: hidden;
    }

    .breakdown-fill {
      height: 100%;
      background: #111827;
    }

    .avatar-fallback {
      width: 2.5rem;
      height: 2.5rem;
      background: #e5e7eb;
      color: #111827;
    }
  `]
})
export class ReviewListComponent {
  @Input() reviews: ListingReview[] = [];
  @Input() averageRating = 0;
  @Input() reviewCount = 0;
  @Input() loading = false;
  @Input() errorMessage = '';
  @Input() canRespondAsHost = false;
  @Input() responseSubmittingFor: string | null = null;
  @Input() responseError = '';

  @Output() hostResponseSubmitted = new EventEmitter<{ reviewId: string; response: string }>();

  expandedResponseReviewId: string | null = null;

  get breakdown() {
    const totalLoaded = this.reviews.length || 1;
    return [5, 4, 3, 2, 1].map(stars => {
      const count = this.reviews.filter(review => review.rating === stars).length;
      return {
        stars,
        count,
        percentage: Math.round((count / totalLoaded) * 100)
      };
    });
  }

  initialsFor(review: ListingReview): string {
    return review.guestDisplayName
      .split(' ')
      .map(part => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  starsFor(rating: number): string[] {
    return Array.from({ length: rating }, () => '★');
  }

  openHostResponse(reviewId: string) {
    this.expandedResponseReviewId = reviewId;
  }

  cancelHostResponse() {
    this.expandedResponseReviewId = null;
  }

  submitHostResponse(reviewId: string, response: string) {
    this.hostResponseSubmitted.emit({ reviewId, response });
  }
}

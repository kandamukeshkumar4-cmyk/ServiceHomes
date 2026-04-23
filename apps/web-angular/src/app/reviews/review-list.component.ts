import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HostResponseFormComponent } from './host-response-form.component';
import { ListingReview } from './reviews-api.service';

export interface ReviewReportSubmission {
  reviewId: string;
  reason: string;
  details: string;
}

@Component({
  selector: 'app-review-list',
  standalone: true,
  imports: [CommonModule, FormsModule, HostResponseFormComponent],
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
  @Input() cleanlinessRating: number | null = null;
  @Input() accuracyRating: number | null = null;
  @Input() communicationRating: number | null = null;
  @Input() locationRating: number | null = null;
  @Input() valueRating: number | null = null;
  @Input() trustScore = 0;
  @Input() loading = false;
  @Input() errorMessage = '';
  @Input() canRespondAsHost = false;
  @Input() responseSubmittingFor: string | null = null;
  @Input() responseError = '';
  @Input() canReportReviews = false;
  @Input() reportingReviewId: string | null = null;
  @Input() reportError = '';

  @Output() hostResponseSubmitted = new EventEmitter<{ reviewId: string; response: string }>();
  @Output() reviewReported = new EventEmitter<ReviewReportSubmission>();

  expandedResponseReviewId: string | null = null;
  expandedReportReviewId: string | null = null;
  reportReason = 'OTHER';
  reportDetails = '';

  readonly reportReasons = [
    { value: 'SPAM', label: 'Spam' },
    { value: 'HATE_OR_HARASSMENT', label: 'Harassment' },
    { value: 'OFF_PLATFORM_PAYMENT', label: 'Off-platform payment' },
    { value: 'PERSONAL_INFORMATION', label: 'Personal information' },
    { value: 'IRRELEVANT', label: 'Irrelevant' },
    { value: 'OTHER', label: 'Other' }
  ];

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

  aggregateRatings() {
    return [
      { label: 'Cleanliness', value: this.cleanlinessRating },
      { label: 'Accuracy', value: this.accuracyRating },
      { label: 'Communication', value: this.communicationRating },
      { label: 'Location', value: this.locationRating },
      { label: 'Value', value: this.valueRating }
    ].filter(item => item.value !== null && item.value !== undefined);
  }

  ratingDetailsFor(review: ListingReview) {
    return [
      { label: 'Cleanliness', value: review.cleanlinessRating },
      { label: 'Accuracy', value: review.accuracyRating },
      { label: 'Communication', value: review.communicationRating },
      { label: 'Location', value: review.locationRating },
      { label: 'Value', value: review.valueRating }
    ].filter(item => item.value !== null && item.value !== undefined);
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

  openReport(reviewId: string) {
    this.expandedReportReviewId = reviewId;
    this.reportReason = 'OTHER';
    this.reportDetails = '';
  }

  cancelReport() {
    this.expandedReportReviewId = null;
    this.reportDetails = '';
  }

  submitReport(reviewId: string) {
    if (this.reportingReviewId === reviewId) {
      return;
    }

    this.reviewReported.emit({
      reviewId,
      reason: this.reportReason,
      details: this.reportDetails.trim()
    });
  }
}

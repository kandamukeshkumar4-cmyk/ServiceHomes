import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReviewRatingBreakdown, ReviewReservationOption } from './reviews-api.service';

export interface ReviewSubmission {
  reservationId: string;
  rating: number;
  breakdown: ReviewRatingBreakdown;
  comment: string;
}

@Component({
  selector: 'app-review-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review-form.component.html',
  styles: [`
    .rating-chip {
      min-width: 2.5rem;
    }

    .rating-chip.active {
      background: #111827;
      border-color: #111827;
      color: #ffffff;
    }
  `]
})
export class ReviewFormComponent implements OnChanges {
  @Input() reservations: ReviewReservationOption[] = [];
  @Input() submitting = false;
  @Input() errorMessage = '';
  @Output() submitted = new EventEmitter<ReviewSubmission>();

  selectedReservationId = '';
  rating = 5;
  breakdown: ReviewRatingBreakdown = {
    cleanlinessRating: 5,
    accuracyRating: 5,
    communicationRating: 5,
    locationRating: 5,
    valueRating: 5
  };
  comment = '';

  readonly ratingCategories: { key: keyof ReviewRatingBreakdown; label: string }[] = [
    { key: 'cleanlinessRating', label: 'Cleanliness' },
    { key: 'accuracyRating', label: 'Accuracy' },
    { key: 'communicationRating', label: 'Communication' },
    { key: 'locationRating', label: 'Location' },
    { key: 'valueRating', label: 'Value' }
  ];

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['reservations']) {
      return;
    }

    const reservationStillExists = this.reservations.some(reservation => reservation.id === this.selectedReservationId);
    if (!reservationStillExists) {
      this.selectedReservationId = this.reservations[0]?.id ?? '';
    }
  }

  reset() {
    this.selectedReservationId = this.reservations[0]?.id ?? '';
    this.rating = 5;
    this.breakdown = {
      cleanlinessRating: 5,
      accuracyRating: 5,
      communicationRating: 5,
      locationRating: 5,
      valueRating: 5
    };
    this.comment = '';
  }

  submit() {
    const normalizedComment = this.comment.trim();
    if (!this.selectedReservationId || !normalizedComment || this.submitting) {
      return;
    }

    this.submitted.emit({
      reservationId: this.selectedReservationId,
      rating: this.rating,
      breakdown: { ...this.breakdown },
      comment: normalizedComment
    });
  }
}

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ListingService } from './listing.service';
import {
  Listing,
  ListingAvailabilityResponse,
  ListingAvailabilityRule,
  ListingAvailabilityRuleType,
  ListingCalendarDay,
  ListingCalendarResponse
} from './listing.model';

@Component({
  selector: 'app-listing-availability',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './listing-availability.component.html',
  styles: [`
    .calendar-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(7rem, 1fr));
      gap: 0.75rem;
    }

    .calendar-day {
      min-height: 7.5rem;
      border: 1px solid var(--surface-border, #dfe7ef);
      border-radius: 1rem;
      padding: 0.75rem;
      background: #fff;
    }

    .calendar-day--blocked {
      background: #fff1f2;
      border-color: #fecdd3;
    }

    .calendar-day--override {
      background: #eff6ff;
      border-color: #bfdbfe;
    }
  `]
})
export class ListingAvailabilityComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);

  listingId = '';
  listing: Listing | null = null;
  availability: ListingAvailabilityResponse | null = null;
  draftRules: ListingAvailabilityRule[] = [];
  calendar: ListingCalendarResponse | null = null;

  calendarStart = this.toDateInput(new Date());
  calendarEnd = this.toDateInput(this.addDays(new Date(), 41));

  newRuleType: ListingAvailabilityRuleType = 'BLOCKED_DATE';
  newRuleStart = '';
  newRuleEnd = '';
  newRuleValue: number | null = null;

  error = '';
  calendarError = '';
  saving = false;

  ngOnInit() {
    this.listingId = this.route.snapshot.paramMap.get('id') || '';
    this.listingService.getById(this.listingId).subscribe({
      next: listing => {
        this.listing = listing;
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to load this listing.';
      }
    });
    this.loadAvailability();
    this.loadCalendar();
  }

  get calendarDays(): ListingCalendarDay[] {
    return this.calendar?.days ?? [];
  }

  get hasUnsavedChanges(): boolean {
    return JSON.stringify(this.draftRules) !== JSON.stringify(this.availability?.rules ?? []);
  }

  loadAvailability() {
    this.listingService.getAvailability(this.listingId).subscribe({
      next: response => {
        this.availability = response;
        this.draftRules = response.rules.map(rule => ({ ...rule, value: rule.value ?? null }));
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to load availability rules.';
      }
    });
  }

  loadCalendar() {
    this.listingService.getCalendar(this.listingId, this.calendarStart, this.calendarEnd).subscribe({
      next: response => {
        this.calendar = response;
        this.calendarError = '';
      },
      error: error => {
        this.calendarError = error?.error?.message || 'Unable to load the calendar preview.';
      }
    });
  }

  addRule() {
    this.error = '';
    if (!this.newRuleStart || !this.newRuleEnd) {
      this.error = 'Choose a start and end date for the rule.';
      return;
    }
    if (this.newRuleEnd < this.newRuleStart) {
      this.error = 'The end date must be on or after the start date.';
      return;
    }
    if (this.requiresValue(this.newRuleType) && (this.newRuleValue === null || this.newRuleValue === undefined || Number.isNaN(this.newRuleValue))) {
      this.error = 'Enter a value for this override.';
      return;
    }
    if (this.newRuleType === 'MIN_NIGHTS_OVERRIDE' && (!Number.isInteger(this.newRuleValue) || (this.newRuleValue ?? 0) <= 0)) {
      this.error = 'Minimum nights overrides must be whole numbers greater than zero.';
      return;
    }
    if (this.newRuleType === 'PRICE_OVERRIDE' && (this.newRuleValue ?? 0) < 0) {
      this.error = 'Price overrides cannot be negative.';
      return;
    }

    this.draftRules = [...this.draftRules, {
      ruleType: this.newRuleType,
      startDate: this.newRuleStart,
      endDate: this.newRuleEnd,
      value: this.requiresValue(this.newRuleType) ? this.newRuleValue : null
    }].sort((left, right) =>
      left.startDate.localeCompare(right.startDate) ||
      left.endDate.localeCompare(right.endDate) ||
      left.ruleType.localeCompare(right.ruleType)
    );

    this.newRuleStart = '';
    this.newRuleEnd = '';
    this.newRuleValue = null;
    this.newRuleType = 'BLOCKED_DATE';
  }

  removeRule(index: number) {
    this.draftRules = this.draftRules.filter((_, currentIndex) => currentIndex !== index);
  }

  resetDraft() {
    this.draftRules = (this.availability?.rules ?? []).map(rule => ({ ...rule, value: rule.value ?? null }));
    this.error = '';
  }

  save() {
    if (this.saving) {
      return;
    }
    this.saving = true;
    this.error = '';
    this.listingService.updateAvailability(this.listingId, this.draftRules).subscribe({
      next: response => {
        this.availability = response;
        this.draftRules = response.rules.map(rule => ({ ...rule, value: rule.value ?? null }));
        this.saving = false;
        this.loadCalendar();
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to save availability rules.';
        this.saving = false;
      }
    });
  }

  refreshCalendar() {
    if (!this.calendarStart || !this.calendarEnd) {
      this.calendarError = 'Choose both a start and end date for the calendar view.';
      return;
    }
    if (this.calendarEnd < this.calendarStart) {
      this.calendarError = 'The calendar end date must be on or after the start date.';
      return;
    }
    this.loadCalendar();
  }

  ruleTypeLabel(ruleType: ListingAvailabilityRuleType): string {
    if (ruleType === 'BLOCKED_DATE') {
      return 'Blocked dates';
    }
    if (ruleType === 'MIN_NIGHTS_OVERRIDE') {
      return 'Minimum nights override';
    }
    return 'Price override';
  }

  ruleValueLabel(rule: ListingAvailabilityRule): string {
    if (rule.ruleType === 'BLOCKED_DATE') {
      return 'Dates unavailable';
    }
    if (rule.ruleType === 'MIN_NIGHTS_OVERRIDE') {
      return `${rule.value} night minimum`;
    }
    return `${this.formatCurrency(rule.value)} nightly`;
  }

  dayCardClasses(day: ListingCalendarDay): Record<string, boolean> {
    return {
      'calendar-day': true,
      'calendar-day--blocked': day.blocked,
      'calendar-day--override': !day.blocked && (day.minNightsOverride || day.priceOverride)
    };
  }

  formatCalendarDay(date: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(new Date(`${date}T00:00:00`));
  }

  formatCalendarWeekday(date: string): string {
    return new Intl.DateTimeFormat('en-US', { weekday: 'short' }).format(new Date(`${date}T00:00:00`));
  }

  formatCurrency(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value ?? 0);
  }

  private requiresValue(ruleType: ListingAvailabilityRuleType): boolean {
    return ruleType !== 'BLOCKED_DATE';
  }

  private addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }

  private toDateInput(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}

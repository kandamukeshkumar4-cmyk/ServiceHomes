import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HostCalendarService } from './host-calendar.service';
import { ListingService } from '../listings/listing.service';
import { Listing, HostCalendarDay, SeasonalPricingTemplate, LengthOfStayDiscount, WeekendMultiplier, TurnoverDay } from '../listings/listing.model';

@Component({
  selector: 'app-host-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './host-calendar.component.html',
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
    .calendar-day--turnover {
      background: #fef3c7;
      border-color: #fde68a;
    }
    .calendar-day--seasonal {
      background: #eff6ff;
      border-color: #bfdbfe;
    }
    .calendar-day--weekend {
      background: #f0fdf4;
      border-color: #bbf7d0;
    }
    .calendar-day--override {
      background: #faf5ff;
      border-color: #e9d5ff;
    }
    .pricing-section {
      border: 1px solid var(--surface-border, #dfe7ef);
      border-radius: 1rem;
      padding: 1rem;
      margin-bottom: 1rem;
      background: #fff;
    }
  `]
})
export class HostCalendarComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);
  private hostCalendarService = inject(HostCalendarService);

  listingId = '';
  listing: Listing | null = null;
  calendarDays: HostCalendarDay[] = [];
  calendarStart = this.toDateInput(new Date());
  calendarEnd = this.toDateInput(this.addDays(new Date(), 41));

  seasonalTemplates: SeasonalPricingTemplate[] = [];
  lengthOfStayDiscounts: LengthOfStayDiscount[] = [];
  weekendMultiplier: WeekendMultiplier | null = null;
  turnoverDay: TurnoverDay | null = null;

  newTemplateName = '';
  newTemplateStart = '';
  newTemplateEnd = '';
  newTemplateMultiplier: number | null = null;

  newDiscountMinNights: number | null = null;
  newDiscountPercent: number | null = null;

  weekendFridayMultiplier: number | null = null;
  weekendSaturdayMultiplier: number | null = null;
  weekendSundayMultiplier: number | null = null;

  turnoverBufferDays: number | null = null;

  error = '';
  calendarError = '';
  saving = false;

  ngOnInit() {
    this.listingId = this.route.snapshot.paramMap.get('id') || '';
    this.listingService.getById(this.listingId).subscribe({
      next: listing => this.listing = listing,
      error: () => this.error = 'Unable to load this listing.'
    });
    this.loadCalendar();
    this.loadSeasonalTemplates();
    this.loadLengthOfStayDiscounts();
    this.loadWeekendMultiplier();
    this.loadTurnoverDay();
  }

  loadCalendar() {
    this.hostCalendarService.getCalendar(this.listingId, this.calendarStart, this.calendarEnd).subscribe({
      next: response => {
        this.calendarDays = response.days;
        this.calendarError = '';
      },
      error: () => this.calendarError = 'Unable to load the calendar.'
    });
  }

  loadSeasonalTemplates() {
    this.hostCalendarService.getSeasonalTemplates(this.listingId).subscribe({
      next: templates => this.seasonalTemplates = templates,
      error: () => this.error = 'Unable to load seasonal pricing templates.'
    });
  }

  loadLengthOfStayDiscounts() {
    this.hostCalendarService.getLengthOfStayDiscounts(this.listingId).subscribe({
      next: discounts => this.lengthOfStayDiscounts = discounts,
      error: () => this.error = 'Unable to load length-of-stay discounts.'
    });
  }

  loadWeekendMultiplier() {
    this.hostCalendarService.getWeekendMultiplier(this.listingId).subscribe({
      next: wm => {
        this.weekendMultiplier = wm;
        this.weekendFridayMultiplier = wm.fridayMultiplier;
        this.weekendSaturdayMultiplier = wm.saturdayMultiplier;
        this.weekendSundayMultiplier = wm.sundayMultiplier;
      },
      error: () => {}
    });
  }

  loadTurnoverDay() {
    this.hostCalendarService.getTurnoverDay(this.listingId).subscribe({
      next: td => {
        this.turnoverDay = td;
        this.turnoverBufferDays = td.bufferDays;
      },
      error: () => {}
    });
  }

  refreshCalendar() {
    if (!this.calendarStart || !this.calendarEnd) {
      this.calendarError = 'Choose both a start and end date.';
      return;
    }
    if (this.calendarEnd < this.calendarStart) {
      this.calendarError = 'The end date must be on or after the start date.';
      return;
    }
    this.loadCalendar();
  }

  addSeasonalTemplate() {
    this.error = '';
    if (!this.newTemplateName || !this.newTemplateStart || !this.newTemplateEnd || this.newTemplateMultiplier === null) {
      this.error = 'Fill in all template fields.';
      return;
    }
    if (this.newTemplateEnd < this.newTemplateStart) {
      this.error = 'Template end date must be on or after start date.';
      return;
    }
    if (this.newTemplateMultiplier <= 0) {
      this.error = 'Multiplier must be greater than zero.';
      return;
    }

    this.saving = true;
    this.hostCalendarService.createSeasonalTemplate(this.listingId, {
      name: this.newTemplateName,
      startDate: this.newTemplateStart,
      endDate: this.newTemplateEnd,
      multiplier: this.newTemplateMultiplier
    }).subscribe({
      next: () => {
        this.newTemplateName = '';
        this.newTemplateStart = '';
        this.newTemplateEnd = '';
        this.newTemplateMultiplier = null;
        this.saving = false;
        this.loadSeasonalTemplates();
        this.loadCalendar();
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Unable to create template.';
        this.saving = false;
      }
    });
  }

  deleteSeasonalTemplate(templateId: string) {
    this.hostCalendarService.deleteSeasonalTemplate(this.listingId, templateId).subscribe({
      next: () => {
        this.loadSeasonalTemplates();
        this.loadCalendar();
      },
      error: (err: any) => this.error = err?.error?.message || 'Unable to delete template.'
    });
  }

  addLengthOfStayDiscount() {
    this.error = '';
    if (this.newDiscountMinNights === null || this.newDiscountPercent === null) {
      this.error = 'Fill in all discount fields.';
      return;
    }
    if (this.newDiscountMinNights <= 0) {
      this.error = 'Minimum nights must be greater than zero.';
      return;
    }
    if (this.newDiscountPercent < 0 || this.newDiscountPercent > 100) {
      this.error = 'Discount percent must be between 0 and 100.';
      return;
    }

    this.saving = true;
    this.hostCalendarService.createLengthOfStayDiscount(this.listingId, {
      minNights: this.newDiscountMinNights,
      discountPercent: this.newDiscountPercent
    }).subscribe({
      next: () => {
        this.newDiscountMinNights = null;
        this.newDiscountPercent = null;
        this.saving = false;
        this.loadLengthOfStayDiscounts();
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Unable to create discount.';
        this.saving = false;
      }
    });
  }

  deleteLengthOfStayDiscount(discountId: string) {
    this.hostCalendarService.deleteLengthOfStayDiscount(this.listingId, discountId).subscribe({
      next: () => this.loadLengthOfStayDiscounts(),
      error: (err: any) => this.error = err?.error?.message || 'Unable to delete discount.'
    });
  }

  saveWeekendMultiplier() {
    this.error = '';
    if (this.weekendFridayMultiplier === null || this.weekendSaturdayMultiplier === null || this.weekendSundayMultiplier === null) {
      this.error = 'Fill in all weekend multiplier fields.';
      return;
    }
    if (this.weekendFridayMultiplier <= 0 || this.weekendSaturdayMultiplier <= 0 || this.weekendSundayMultiplier <= 0) {
      this.error = 'Multipliers must be greater than zero.';
      return;
    }

    this.saving = true;
    this.hostCalendarService.updateWeekendMultiplier(this.listingId, {
      fridayMultiplier: this.weekendFridayMultiplier,
      saturdayMultiplier: this.weekendSaturdayMultiplier,
      sundayMultiplier: this.weekendSundayMultiplier
    }).subscribe({
      next: () => {
        this.saving = false;
        this.loadWeekendMultiplier();
        this.loadCalendar();
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Unable to update weekend multiplier.';
        this.saving = false;
      }
    });
  }

  saveTurnoverDay() {
    this.error = '';
    if (this.turnoverBufferDays === null || this.turnoverBufferDays < 0) {
      this.error = 'Buffer days cannot be negative.';
      return;
    }

    this.saving = true;
    this.hostCalendarService.updateTurnoverDay(this.listingId, {
      bufferDays: this.turnoverBufferDays
    }).subscribe({
      next: () => {
        this.saving = false;
        this.loadTurnoverDay();
        this.loadCalendar();
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Unable to update turnover days.';
        this.saving = false;
      }
    });
  }

  dayCardClasses(day: HostCalendarDay): Record<string, boolean> {
    return {
      'calendar-day': true,
      'calendar-day--blocked': day.blocked,
      'calendar-day--turnover': day.turnover,
      'calendar-day--override': day.hasPriceOverride,
      'calendar-day--seasonal': day.hasSeasonalTemplate && !day.hasPriceOverride,
      'calendar-day--weekend': day.hasWeekendMultiplier && !day.hasPriceOverride
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

  private addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }

  private toDateInput(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}

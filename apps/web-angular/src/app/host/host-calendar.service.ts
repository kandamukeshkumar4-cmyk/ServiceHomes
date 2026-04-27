import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HostCalendarResponse,
  SeasonalPricingTemplate,
  LengthOfStayDiscount,
  WeekendMultiplier,
  TurnoverDay,
  CreateSeasonalPricingTemplateRequest,
  CreateLengthOfStayDiscountRequest,
  UpdateWeekendMultiplierRequest,
  UpdateTurnoverDayRequest
} from '../listings/listing.model';

@Injectable({ providedIn: 'root' })
export class HostCalendarService {
  private http = inject(HttpClient);

  getCalendar(listingId: string, startDate?: string, endDate?: string): Observable<HostCalendarResponse> {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    const query = params.toString();
    return this.http.get<HostCalendarResponse>(`/api/host/listings/${listingId}/calendar${query ? `?${query}` : ''}`);
  }

  getSeasonalTemplates(listingId: string): Observable<SeasonalPricingTemplate[]> {
    return this.http.get<SeasonalPricingTemplate[]>(`/api/host/listings/${listingId}/calendar/seasonal-templates`);
  }

  createSeasonalTemplate(listingId: string, request: CreateSeasonalPricingTemplateRequest): Observable<SeasonalPricingTemplate> {
    return this.http.post<SeasonalPricingTemplate>(`/api/host/listings/${listingId}/calendar/seasonal-templates`, request);
  }

  updateSeasonalTemplate(listingId: string, templateId: string, request: CreateSeasonalPricingTemplateRequest): Observable<SeasonalPricingTemplate> {
    return this.http.put<SeasonalPricingTemplate>(`/api/host/listings/${listingId}/calendar/seasonal-templates/${templateId}`, request);
  }

  deleteSeasonalTemplate(listingId: string, templateId: string): Observable<void> {
    return this.http.delete<void>(`/api/host/listings/${listingId}/calendar/seasonal-templates/${templateId}`);
  }

  getLengthOfStayDiscounts(listingId: string): Observable<LengthOfStayDiscount[]> {
    return this.http.get<LengthOfStayDiscount[]>(`/api/host/listings/${listingId}/calendar/length-of-stay-discounts`);
  }

  createLengthOfStayDiscount(listingId: string, request: CreateLengthOfStayDiscountRequest): Observable<LengthOfStayDiscount> {
    return this.http.post<LengthOfStayDiscount>(`/api/host/listings/${listingId}/calendar/length-of-stay-discounts`, request);
  }

  updateLengthOfStayDiscount(listingId: string, discountId: string, request: CreateLengthOfStayDiscountRequest): Observable<LengthOfStayDiscount> {
    return this.http.put<LengthOfStayDiscount>(`/api/host/listings/${listingId}/calendar/length-of-stay-discounts/${discountId}`, request);
  }

  deleteLengthOfStayDiscount(listingId: string, discountId: string): Observable<void> {
    return this.http.delete<void>(`/api/host/listings/${listingId}/calendar/length-of-stay-discounts/${discountId}`);
  }

  getWeekendMultiplier(listingId: string): Observable<WeekendMultiplier> {
    return this.http.get<WeekendMultiplier>(`/api/host/listings/${listingId}/calendar/weekend-multiplier`);
  }

  updateWeekendMultiplier(listingId: string, request: UpdateWeekendMultiplierRequest): Observable<WeekendMultiplier> {
    return this.http.put<WeekendMultiplier>(`/api/host/listings/${listingId}/calendar/weekend-multiplier`, request);
  }

  getTurnoverDay(listingId: string): Observable<TurnoverDay> {
    return this.http.get<TurnoverDay>(`/api/host/listings/${listingId}/calendar/turnover-day`);
  }

  updateTurnoverDay(listingId: string, request: UpdateTurnoverDayRequest): Observable<TurnoverDay> {
    return this.http.put<TurnoverDay>(`/api/host/listings/${listingId}/calendar/turnover-day`, request);
  }
}

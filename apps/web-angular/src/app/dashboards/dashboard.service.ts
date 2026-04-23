import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReservationPipelineItem {
  reservationId: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string | null;
  listingCity: string | null;
  listingCountry: string | null;
  guestId: string;
  guestDisplayName: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  totalAmount: number;
  status: string;
}

export interface ListingPerformanceItem {
  listingId: string;
  listingTitle: string;
  coverUrl: string | null;
  bookingCount: number;
  averageRating: number | null;
  reviewCount: number;
}

export interface HostDashboardData {
  upcomingReservations: ReservationPipelineItem[];
  pendingRequests: ReservationPipelineItem[];
  occupancyRate: number;
  mockEarnings: number;
  listingPerformance: ListingPerformanceItem[];
  unreadMessageThreads: number;
}

export interface TripItem {
  reservationId: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string | null;
  listingCity: string | null;
  listingCountry: string | null;
  hostId: string;
  hostDisplayName: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  totalAmount: number;
  status: string;
  canReview: boolean;
}

export interface GuestDashboardData {
  upcomingTrips: TripItem[];
  pastTrips: TripItem[];
  savedListingsCount: number;
  unreadMessageThreads: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  getHostDashboard(): Observable<HostDashboardData> {
    return this.http.get<HostDashboardData>('/api/host/dashboard');
  }

  getGuestDashboard(): Observable<GuestDashboardData> {
    return this.http.get<GuestDashboardData>('/api/guest/dashboard');
  }
}

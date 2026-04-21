import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';

interface Booking {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string;
  listingCity: string;
  listingCountry: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  totalAmount: number;
  status: string;
  hostDisplayName: string;
}

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-bookings.component.html',
  styles: []
})
export class MyBookingsComponent implements OnInit {
  private http = inject(HttpClient);
  bookings: Booking[] = [];

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<Booking[]>('/api/reservations/my').subscribe(data => this.bookings = data);
  }

  canCancel(status: string): boolean {
    return status === 'PENDING' || status === 'CONFIRMED';
  }

  cancel(id: string) {
    this.http.post(`/api/reservations/${id}/cancel`, {}).subscribe(() => this.load());
  }
}

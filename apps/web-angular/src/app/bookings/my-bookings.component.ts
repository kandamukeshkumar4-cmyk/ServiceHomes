import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ReservationPage, ReservationRecord } from './reservation.model';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-bookings.component.html',
  styles: []
})
export class MyBookingsComponent implements OnInit {
  private http = inject(HttpClient);
  bookings: ReservationRecord[] = [];
  totalBookings = 0;

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<ReservationPage>('/api/reservations/my').subscribe(page => {
      this.bookings = page.content;
      this.totalBookings = page.totalElements;
    });
  }

  canCancel(status: string): boolean {
    return status === 'PENDING' || status === 'CONFIRMED';
  }

  statusLabel(status: string): string {
    if (status === 'PENDING') return 'Awaiting host approval';
    if (status === 'DECLINED') return 'Declined';
    return status.replaceAll('_', ' ');
  }

  cancel(id: string) {
    this.http.post(`/api/reservations/${id}/cancel`, {}).subscribe(() => this.load());
  }
}

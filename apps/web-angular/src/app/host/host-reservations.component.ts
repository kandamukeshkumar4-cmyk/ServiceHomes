import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ReservationPage, ReservationRecord } from '../bookings/reservation.model';

@Component({
  selector: 'app-host-reservations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './host-reservations.component.html',
  styles: []
})
export class HostReservationsComponent implements OnInit {
  private http = inject(HttpClient);
  reservations: ReservationRecord[] = [];
  totalReservations = 0;

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<ReservationPage>('/api/reservations/host').subscribe(page => {
      this.reservations = page.content;
      this.totalReservations = page.totalElements;
    });
  }

  canCancel(status: string): boolean {
    return status === 'CONFIRMED';
  }

  canAcceptOrDecline(status: string): boolean {
    return status === 'PENDING';
  }

  statusLabel(status: string): string {
    if (status === 'PENDING') return 'Needs approval';
    if (status === 'DECLINED') return 'Declined';
    return status.replaceAll('_', ' ');
  }

  accept(id: string) {
    this.http.post(`/api/reservations/${id}/accept`, {}).subscribe(() => this.load());
  }

  decline(id: string) {
    this.http.post(`/api/reservations/${id}/decline`, {}).subscribe(() => this.load());
  }

  cancel(id: string) {
    this.http.post(`/api/reservations/${id}/cancel-by-host`, {}).subscribe(() => this.load());
  }
}

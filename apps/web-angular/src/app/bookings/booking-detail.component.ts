import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ReservationRecord } from './reservation.model';

@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './booking-detail.component.html',
  styles: []
})
export class BookingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private router = inject(Router);

  booking: ReservationRecord | null = null;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.http.get<ReservationRecord>(`/api/reservations/${id}`).subscribe(b => this.booking = b);
  }

  canCancel(): boolean {
    return this.booking ? ['PENDING', 'CONFIRMED'].includes(this.booking.status) : false;
  }

  canMessageHost(): boolean {
    return this.booking ? ['PENDING', 'CONFIRMED'].includes(this.booking.status) : false;
  }

  statusLabel(status: string): string {
    if (status === 'PENDING') return 'Awaiting host approval';
    if (status === 'DECLINED') return 'Declined by host';
    return status.replaceAll('_', ' ');
  }

  statusMessage(): string {
    if (!this.booking) {
      return '';
    }
    if (this.booking.status === 'PENDING') {
      return 'Your reservation request is waiting for the host to accept or decline it.';
    }
    if (this.booking.status === 'CONFIRMED') {
      return 'Your stay is confirmed. No payment was collected in this demo flow.';
    }
    if (this.booking.status === 'DECLINED') {
      return 'The host declined this request. You can head back to the listing and try different dates.';
    }
    if (this.booking.status.includes('CANCELLED')) {
      return 'This reservation is no longer active.';
    }
    return '';
  }

  cancel() {
    if (!this.booking) return;
    this.http.post(`/api/reservations/${this.booking.id}/cancel`, {}).subscribe(() => {
      this.router.navigate(['/bookings']);
    });
  }
}

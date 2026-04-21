import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface BookingDetail {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string;
  listingCity: string;
  listingCountry: string;
  guestId: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  nightlyPrice: number;
  cleaningFee: number;
  serviceFee: number;
  totalAmount: number;
  status: string;
  hostDisplayName: string;
}

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

  booking: BookingDetail | null = null;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.http.get<BookingDetail>(`/api/reservations/${id}`).subscribe(b => this.booking = b);
  }

  canCancel(): boolean {
    return this.booking ? ['PENDING', 'CONFIRMED'].includes(this.booking.status) : false;
  }

  cancel() {
    if (!this.booking) return;
    this.http.post(`/api/reservations/${this.booking.id}/cancel`, {}).subscribe(() => {
      this.router.navigate(['/bookings']);
    });
  }
}

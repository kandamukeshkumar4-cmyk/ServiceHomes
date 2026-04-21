import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface HostReservation {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  totalAmount: number;
  status: string;
}

@Component({
  selector: 'app-host-reservations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './host-reservations.component.html',
  styles: []
})
export class HostReservationsComponent implements OnInit {
  private http = inject(HttpClient);
  reservations: HostReservation[] = [];

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<HostReservation[]>('/api/reservations/host').subscribe(data => this.reservations = data);
  }

  canCancel(status: string): boolean {
    return status === 'PENDING' || status === 'CONFIRMED';
  }

  cancel(id: string) {
    this.http.post(`/api/reservations/${id}/cancel-by-host`, {}).subscribe(() => this.load());
  }
}

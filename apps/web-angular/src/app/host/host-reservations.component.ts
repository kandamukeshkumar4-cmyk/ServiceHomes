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
  template: `
    <div class="p-4 max-w-60rem mx-auto">
      <h1 class="text-2xl font-bold mb-4">Reservations</h1>

      <div *ngIf="reservations.length === 0" class="surface-100 p-4 border-round-lg text-center">
        <p class="text-600">No reservations yet.</p>
      </div>

      <div class="flex flex-column gap-3">
        <div *ngFor="let res of reservations" class="surface-0 shadow-1 border-round-lg overflow-hidden flex flex-column md:flex-row">
          <div class="h-10rem md:h-auto md:w-12rem surface-200 flex-shrink-0">
            <img *ngIf="res.listingCoverUrl" [src]="res.listingCoverUrl" class="w-full h-full object-cover" />
          </div>
          <div class="p-3 flex-1 flex flex-column justify-content-between">
            <div>
              <div class="flex justify-content-between align-items-start mb-1">
                <h3 class="text-lg font-bold m-0">{{ res.listingTitle }}</h3>
                <span class="px-2 py-1 text-xs border-round-lg"
                      [class.bg-green-100]="res.status === 'CONFIRMED'"
                      [class.text-green-700]="res.status === 'CONFIRMED'"
                      [class.bg-yellow-100]="res.status === 'PENDING'"
                      [class.text-yellow-700]="res.status === 'PENDING'"
                      [class.bg-red-100]="res.status.includes('CANCELLED')"
                      [class.text-red-700]="res.status.includes('CANCELLED')">
                  {{ res.status }}
                </span>
              </div>
              <p class="text-600 text-sm m-0">{{ res.checkIn }} to {{ res.checkOut }} · {{ res.guests }} guests · {{ res.totalNights }} nights</p>
            </div>
            <div class="flex justify-content-between align-items-center mt-2">
              <span class="font-bold">${{ res.totalAmount }}</span>
              <button *ngIf="canCancel(res.status)" class="p-button p-button-sm p-button-danger" (click)="cancel(res.id)">Cancel</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
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

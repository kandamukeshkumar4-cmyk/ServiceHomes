import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ListingService } from '../listings/listing.service';
import { Listing, ListingPhoto } from '../listings/listing.model';
import { AppAuthService } from '../core/auth.service';
import { ListingMapComponent } from './listing-map.component';



@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ListingMapComponent],
  templateUrl: './listing-detail.component.html',
  styles: []
})
export class ListingDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private listingService = inject(ListingService);
  private http = inject(HttpClient);
  auth = inject(AppAuthService);

  listing: Listing | null = null;
  checkIn = '';
  checkOut = '';
  guests = 1;
  quoteNights = 0;
  quoteNightlyPrice = 0;
  quoteSubtotal = 0;
  quoteCleaningFee = 0;
  quoteServiceFee = 0;
  quoteTotal = 0;
  showQuote = false;

  get coverPhoto(): ListingPhoto | undefined {
    return this.listing?.photos.find(p => p.isCover) || this.listing?.photos[0];
  }

  get otherPhotos(): ListingPhoto[] {
    return (this.listing?.photos || []).filter(p => p.id !== this.coverPhoto?.id).slice(0, 3);
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.listingService.getById(id).subscribe(l => {
      this.listing = l;
    });
  }

  reserve() {
    if (!this.listing || !this.checkIn || !this.checkOut) return;
    this.http.post('/api/reservations', {
      listingId: this.listing.id,
      checkIn: this.checkIn,
      checkOut: this.checkOut,
      guests: this.guests
    }).subscribe(() => {
      alert('Reservation created!');
    });
  }
}

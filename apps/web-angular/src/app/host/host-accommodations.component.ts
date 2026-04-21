import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ListingService } from '../listings/listing.service';
import { Listing } from '../listings/listing.model';

@Component({
  selector: 'app-host-accommodations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './host-accommodations.component.html',
  styles: []
})
export class HostAccommodationsComponent implements OnInit {
  private listingService = inject(ListingService);
  listings: Listing[] = [];

  ngOnInit() {
    this.load();
  }

  load() {
    this.listingService.getMyListings().subscribe(data => this.listings = data);
  }

  publish(id: string) {
    this.listingService.publish(id).subscribe(() => this.load());
  }

  unpublish(id: string) {
    this.listingService.unpublish(id).subscribe(() => this.load());
  }
}

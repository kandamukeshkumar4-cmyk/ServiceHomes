import { Component, Input, OnChanges, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';

@Component({
  selector: 'app-listing-map',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="surface-0 shadow-1 border-round-lg overflow-hidden">
      <div #mapContainer class="map-container"></div>
    </div>
  `,
  styles: [`
    .map-container {
      height: 300px;
      width: 100%;
    }
  `]
})
export class ListingMapComponent implements OnChanges {
  @Input() latitude?: number;
  @Input() longitude?: number;
  @Input() title?: string;
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  private map: L.Map | null = null;

  ngOnChanges() {
    if (this.latitude && this.longitude) {
      this.initMap();
    }
  }

  private initMap() {
    if (this.map) {
      this.map.remove();
    }

    this.map = L.map(this.mapContainer.nativeElement, {
      zoomControl: true,
      scrollWheelZoom: false
    }).setView([this.latitude!, this.longitude!], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    const marker = L.marker([this.latitude!, this.longitude!]).addTo(this.map);
    if (this.title) {
      marker.bindPopup(this.title).openPopup();
    }

    setTimeout(() => {
      this.map?.invalidateSize();
    }, 100);
  }
}

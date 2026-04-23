import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  inject
} from '@angular/core';
import * as L from 'leaflet';
import { ListingSearchResult } from '../listings/listing.model';
import { SearchFilters } from './search-filters.model';

export interface SearchMapBounds {
  swLat: number;
  swLng: number;
  neLat: number;
  neLng: number;
}

@Component({
  selector: 'app-search-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-map.component.html',
  styleUrl: './search-map.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly zone = inject(NgZone);

  @ViewChild('mapContainer', { static: true }) private mapContainer!: ElementRef<HTMLDivElement>;

  @Input() results: readonly ListingSearchResult[] = [];
  @Input() filters: SearchFilters | null = null;
  @Input() loading = false;

  @Output() readonly searchAreaRequested = new EventEmitter<SearchMapBounds>();

  readonly visibleResultsLabel = 'Listings on map';
  isSearchAreaDirty = false;

  private map: L.Map | null = null;
  private readonly markerLayer = L.layerGroup();
  private readonly overlayLayer = L.layerGroup();
  private readonly defaultCenter: L.LatLngTuple = [39.8283, -98.5795];
  private readonly defaultZoom = 4;
  private latestResultBounds: L.LatLngBounds | null = null;
  private pendingBounds: SearchMapBounds | null = null;
  private suppressUserViewportEvents = false;
  private hasAppliedInitialViewport = false;

  ngAfterViewInit(): void {
    this.initializeMap();
    this.renderMapState();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }

    if (changes['results'] || changes['filters']) {
      this.renderMapState();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  get visibleResultCount(): number {
    return this.results.length;
  }

  get hasResults(): boolean {
    return this.results.some((listing) => isFiniteCoordinate(listing.latitude) && isFiniteCoordinate(listing.longitude));
  }

  requestSearchArea(): void {
    if (!this.pendingBounds) {
      return;
    }

    this.searchAreaRequested.emit(this.pendingBounds);
    this.isSearchAreaDirty = false;
    this.changeDetectorRef.markForCheck();
  }

  private initializeMap(): void {
    const map = L.map(this.mapContainer.nativeElement, {
      zoomControl: true,
      scrollWheelZoom: true
    }).setView(this.defaultCenter, this.defaultZoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    this.markerLayer.addTo(map);
    this.overlayLayer.addTo(map);
    this.map = map;

    map.on('moveend zoomend', () => {
      this.zone.run(() => {
        if (this.suppressUserViewportEvents) {
          return;
        }

        const currentBounds = this.readMapBounds();
        const filterBounds = getFilterBounds(this.filters);
        this.pendingBounds = currentBounds;
        this.isSearchAreaDirty = !areBoundsEqual(currentBounds, filterBounds);
        this.changeDetectorRef.markForCheck();
      });
    });

    window.setTimeout(() => {
      map.invalidateSize();
    }, 0);
  }

  private renderMapState(): void {
    this.renderMarkers();
    this.renderActiveSearchArea();
    this.syncViewportToInputs();
    this.changeDetectorRef.markForCheck();
  }

  private renderMarkers(): void {
    this.markerLayer.clearLayers();

    const coordinates: L.LatLngExpression[] = [];
    for (const listing of this.results) {
      if (!isFiniteCoordinate(listing.latitude) || !isFiniteCoordinate(listing.longitude)) {
        continue;
      }

      const coordinatesTuple: L.LatLngTuple = [listing.latitude, listing.longitude];
      coordinates.push(coordinatesTuple);

      const marker = L.marker(coordinatesTuple, {
        icon: createPriceIcon(listing.nightlyPrice)
      });

      marker.bindPopup(this.buildPopupContent(listing), {
        autoPanPadding: L.point(24, 24),
        closeButton: true,
        maxWidth: 280
      });

      marker.addTo(this.markerLayer);
    }

    this.latestResultBounds = coordinates.length > 0 ? L.latLngBounds(coordinates) : null;
  }

  private renderActiveSearchArea(): void {
    this.overlayLayer.clearLayers();

    const filters = this.filters;
    if (!filters) {
      return;
    }

    const filterBounds = getFilterBounds(filters);
    if (filterBounds) {
      L.rectangle(
        [
          [filterBounds.swLat, filterBounds.swLng],
          [filterBounds.neLat, filterBounds.neLng]
        ],
        {
          color: '#2563eb',
          weight: 2,
          fillColor: '#60a5fa',
          fillOpacity: 0.08
        }
      ).addTo(this.overlayLayer);
      return;
    }

    if (isFiniteCoordinate(filters.lat) && isFiniteCoordinate(filters.lng) && typeof filters.radiusKm === 'number') {
      L.circle([filters.lat, filters.lng], {
        radius: filters.radiusKm * 1000,
        color: '#2563eb',
        weight: 2,
        fillColor: '#93c5fd',
        fillOpacity: 0.12
      }).addTo(this.overlayLayer);
    }
  }

  private syncViewportToInputs(): void {
    if (!this.map) {
      return;
    }

    const filterBounds = getFilterBounds(this.filters);
    if (filterBounds) {
      this.withSuppressedViewportEvents(() => {
        this.map?.fitBounds(
          [
            [filterBounds.swLat, filterBounds.swLng],
            [filterBounds.neLat, filterBounds.neLng]
          ],
          { padding: [36, 36] }
        );
      });
      this.pendingBounds = filterBounds;
      this.isSearchAreaDirty = false;
      this.hasAppliedInitialViewport = true;
      return;
    }

    if (this.filters && isFiniteCoordinate(this.filters.lat) && isFiniteCoordinate(this.filters.lng)) {
      if (typeof this.filters.radiusKm === 'number' && this.filters.radiusKm > 0) {
        const circleBounds = L.circle([this.filters.lat, this.filters.lng], {
          radius: this.filters.radiusKm * 1000
        }).getBounds();
        this.withSuppressedViewportEvents(() => {
          this.map?.fitBounds(circleBounds, { padding: [36, 36] });
        });
      } else {
        this.withSuppressedViewportEvents(() => {
          this.map?.setView([this.filters!.lat!, this.filters!.lng!], 12);
        });
      }

      this.pendingBounds = this.readMapBounds();
      this.isSearchAreaDirty = false;
      this.hasAppliedInitialViewport = true;
      return;
    }

    if (this.latestResultBounds) {
      this.withSuppressedViewportEvents(() => {
        this.map?.fitBounds(this.latestResultBounds!, { padding: [36, 36], maxZoom: 13 });
      });
      this.pendingBounds = this.readMapBounds();
      this.isSearchAreaDirty = false;
      this.hasAppliedInitialViewport = true;
      return;
    }

    if (!this.hasAppliedInitialViewport) {
      this.withSuppressedViewportEvents(() => {
        this.map?.setView(this.defaultCenter, this.defaultZoom);
      });
      this.pendingBounds = this.readMapBounds();
      this.isSearchAreaDirty = false;
      this.hasAppliedInitialViewport = true;
    }
  }

  private withSuppressedViewportEvents(work: () => void): void {
    if (!this.map) {
      return;
    }

    this.suppressUserViewportEvents = true;
    this.map.once('moveend', () => {
      this.suppressUserViewportEvents = false;
    });
    work();
    window.setTimeout(() => {
      this.suppressUserViewportEvents = false;
    }, 250);
  }

  private readMapBounds(): SearchMapBounds | null {
    if (!this.map) {
      return null;
    }

    const bounds = this.map.getBounds();
    return {
      swLat: roundCoordinate(bounds.getSouthWest().lat),
      swLng: roundCoordinate(bounds.getSouthWest().lng),
      neLat: roundCoordinate(bounds.getNorthEast().lat),
      neLng: roundCoordinate(bounds.getNorthEast().lng)
    };
  }

  private buildPopupContent(listing: ListingSearchResult): HTMLElement {
    const wrapper = document.createElement('article');
    wrapper.style.width = '240px';
    wrapper.style.display = 'grid';
    wrapper.style.gap = '0.75rem';

    if (listing.coverUrl) {
      const image = document.createElement('img');
      image.src = listing.coverUrl;
      image.alt = listing.title;
      image.loading = 'lazy';
      image.style.width = '100%';
      image.style.height = '132px';
      image.style.objectFit = 'cover';
      image.style.borderRadius = '12px';
      wrapper.appendChild(image);
    }

    const title = document.createElement('h3');
    title.textContent = listing.title;
    title.style.margin = '0';
    title.style.fontSize = '1rem';
    title.style.fontWeight = '600';
    title.style.lineHeight = '1.4';
    wrapper.appendChild(title);

    const meta = document.createElement('p');
    meta.textContent = `${listing.city}, ${listing.country}`;
    meta.style.margin = '0';
    meta.style.fontSize = '0.875rem';
    meta.style.color = '#475569';
    wrapper.appendChild(meta);

    const footer = document.createElement('div');
    footer.style.display = 'flex';
    footer.style.justifyContent = 'space-between';
    footer.style.alignItems = 'center';
    footer.style.gap = '0.75rem';

    const price = document.createElement('strong');
    price.textContent = `${formatPrice(listing.nightlyPrice)} / night`;
    price.style.fontSize = '0.95rem';
    footer.appendChild(price);

    const rating = document.createElement('span');
    rating.textContent = formatRating(listing.averageRating, listing.reviewCount);
    rating.style.fontSize = '0.875rem';
    rating.style.color = '#0f172a';
    footer.appendChild(rating);

    wrapper.appendChild(footer);
    return wrapper;
  }
}

function createPriceIcon(price: number): L.DivIcon {
  return L.divIcon({
    className: 'search-map-price-marker',
    html: `
      <div style="
        background:#ffffff;
        border:1px solid rgba(15, 23, 42, 0.18);
        border-radius:999px;
        box-shadow:0 8px 24px rgba(15, 23, 42, 0.18);
        color:#0f172a;
        font-size:0.875rem;
        font-weight:700;
        line-height:1;
        padding:0.55rem 0.75rem;
        white-space:nowrap;
      ">
        ${escapeHtml(formatPrice(price))}
      </div>
    `,
    iconAnchor: [28, 18],
    popupAnchor: [0, -18]
  });
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(price);
}

function formatRating(averageRating: number | null | undefined, reviewCount: number | null | undefined): string {
  if (!averageRating || !reviewCount) {
    return 'New';
  }

  return `★ ${averageRating.toFixed(1)} (${reviewCount})`;
}

function getFilterBounds(filters: SearchFilters | null): SearchMapBounds | null {
  if (!filters) {
    return null;
  }

  if (
    !isFiniteCoordinate(filters.swLat) ||
    !isFiniteCoordinate(filters.swLng) ||
    !isFiniteCoordinate(filters.neLat) ||
    !isFiniteCoordinate(filters.neLng)
  ) {
    return null;
  }

  return {
    swLat: roundCoordinate(filters.swLat),
    swLng: roundCoordinate(filters.swLng),
    neLat: roundCoordinate(filters.neLat),
    neLng: roundCoordinate(filters.neLng)
  };
}

function areBoundsEqual(left: SearchMapBounds | null, right: SearchMapBounds | null): boolean {
  if (!left || !right) {
    return false;
  }

  return left.swLat === right.swLat
    && left.swLng === right.swLng
    && left.neLat === right.neLat
    && left.neLng === right.neLng;
}

function roundCoordinate(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000;
}

function isFiniteCoordinate(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value);
}

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

import { Injectable, inject } from '@angular/core';
import { NavigationEnd, Params, Router } from '@angular/router';
import { BehaviorSubject, Subject, filter, map, startWith, debounceTime, distinctUntilChanged } from 'rxjs';
import { DEFAULT_SEARCH_FILTERS, SearchFilters, SearchSort } from './search-filters.model';

@Injectable({ providedIn: 'root' })
export class SearchStateService {
  private readonly router = inject(Router);
  private readonly filtersSubject = new BehaviorSubject<SearchFilters>(cloneFilters(DEFAULT_SEARCH_FILTERS));
  private readonly outboundUpdatesSubject = new Subject<SearchFilters>();

  readonly filters$ = this.filtersSubject.pipe(map(cloneFilters));

  constructor() {
    this.bindRouterState();
    this.bindOutboundUpdates();
  }

  get snapshot(): SearchFilters {
    return cloneFilters(this.filtersSubject.value);
  }

  setFilters(filters: SearchFilters): void {
    this.pushFilters(normalizeFilters(filters));
  }

  patchFilters(patch: Partial<SearchFilters>, resetPage = shouldResetPage(patch)): void {
    const next = normalizeFilters({ ...this.snapshot, ...patch });
    const nextFilters = resetPage ? { ...next, page: DEFAULT_SEARCH_FILTERS.page } : next;
    this.pushFilters(nextFilters);
  }

  removeFilter(key: keyof SearchFilters): void {
    this.patchFilters(
      { [key]: DEFAULT_SEARCH_FILTERS[key] } as Partial<SearchFilters>,
      !['page', 'size', 'sort'].includes(key)
    );
  }

  clearFilters(): void {
    this.pushFilters(DEFAULT_SEARCH_FILTERS);
  }

  parseQueryParams(params: Params): SearchFilters {
    return normalizeFilters({
      listingId: readString(params['listingId']),
      locationQuery: readString(params['locationQuery']) ?? '',
      checkIn: readDateString(params['checkIn']),
      checkOut: readDateString(params['checkOut']),
      guests: readPositiveInteger(params['guests']) ?? DEFAULT_SEARCH_FILTERS.guests,
      minPrice: readNumber(params['minPrice']),
      maxPrice: readNumber(params['maxPrice']),
      lat: readLatitude(params['lat']),
      lng: readLongitude(params['lng']),
      radiusKm: readPositiveNumber(params['radiusKm']),
      bedrooms: readPositiveInteger(params['bedrooms']),
      beds: readPositiveInteger(params['beds']),
      bathrooms: readPositiveInteger(params['bathrooms']),
      propertyTypes: readStringArray(params['propertyTypes']),
      amenityIds: readStringArray(params['amenityIds']),
      instantBook: readBoolean(params['instantBook']),
      swLat: readLatitude(params['swLat']),
      swLng: readLongitude(params['swLng']),
      neLat: readLatitude(params['neLat']),
      neLng: readLongitude(params['neLng']),
      sort: readSort(params['sort']),
      page: readNonNegativeInteger(params['page']) ?? DEFAULT_SEARCH_FILTERS.page,
      size: readPositiveInteger(params['size']) ?? DEFAULT_SEARCH_FILTERS.size
    });
  }

  toQueryParams(filters: SearchFilters): Params {
    return serializeFilters(normalizeFilters(filters));
  }

  private bindRouterState(): void {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      startWith(null),
      map(() => this.parseQueryParams(this.router.routerState.snapshot.root.queryParams)),
      distinctUntilChanged(areFiltersEqual)
    ).subscribe((filters) => {
      if (!areFiltersEqual(filters, this.snapshot)) {
        this.filtersSubject.next(cloneFilters(filters));
      }
    });
  }

  private bindOutboundUpdates(): void {
    this.outboundUpdatesSubject.pipe(
      map((filters) => this.toQueryParams(filters)),
      debounceTime(300),
      distinctUntilChanged(areQueryParamsEqual)
    ).subscribe((queryParams) => {
      const current = this.router.routerState.snapshot.root.queryParams;
      if (areQueryParamsEqual(current, queryParams)) {
        return;
      }

      const urlTree = this.router.parseUrl(this.router.url);
      urlTree.queryParams = queryParams;
      void this.router.navigateByUrl(urlTree);
    });
  }

  private pushFilters(filters: SearchFilters): void {
    if (areFiltersEqual(filters, this.snapshot)) {
      return;
    }

    const nextFilters = cloneFilters(filters);
    this.filtersSubject.next(nextFilters);
    this.outboundUpdatesSubject.next(nextFilters);
  }
}

function cloneFilters(filters: SearchFilters): SearchFilters {
  return { ...filters, propertyTypes: [...filters.propertyTypes], amenityIds: [...filters.amenityIds] };
}

function normalizeFilters(filters: Partial<SearchFilters>): SearchFilters {
  const minPrice = readNonNegativeNumber(filters.minPrice) ?? null;
  const maxPrice = readNonNegativeNumber(filters.maxPrice) ?? null;

  return {
    listingId: readString(filters.listingId),
    locationQuery: readString(filters.locationQuery) ?? '',
    checkIn: readDateString(filters.checkIn),
    checkOut: readDateString(filters.checkOut),
    guests: readPositiveInteger(filters.guests) ?? DEFAULT_SEARCH_FILTERS.guests,
    minPrice,
    maxPrice,
    lat: readLatitude(filters.lat),
    lng: readLongitude(filters.lng),
    radiusKm: readPositiveNumber(filters.radiusKm),
    bedrooms: readPositiveInteger(filters.bedrooms),
    beds: readPositiveInteger(filters.beds),
    bathrooms: readPositiveInteger(filters.bathrooms),
    propertyTypes: readStringArray(filters.propertyTypes),
    amenityIds: readStringArray(filters.amenityIds),
    instantBook: Boolean(filters.instantBook),
    swLat: readLatitude(filters.swLat),
    swLng: readLongitude(filters.swLng),
    neLat: readLatitude(filters.neLat),
    neLng: readLongitude(filters.neLng),
    sort: readSort(filters.sort),
    page: readNonNegativeInteger(filters.page) ?? DEFAULT_SEARCH_FILTERS.page,
    size: readPositiveInteger(filters.size) ?? DEFAULT_SEARCH_FILTERS.size
  };
}

function serializeFilters(filters: SearchFilters): Params {
  const params: Params = {};

  assignIfPresent(params, 'listingId', filters.listingId);
  assignIfPresent(params, 'locationQuery', emptyToNull(filters.locationQuery));
  assignIfPresent(params, 'checkIn', filters.checkIn);
  assignIfPresent(params, 'checkOut', filters.checkOut);
  assignIfDifferent(params, 'guests', filters.guests, DEFAULT_SEARCH_FILTERS.guests);
  assignIfPresent(params, 'minPrice', filters.minPrice);
  assignIfPresent(params, 'maxPrice', filters.maxPrice);
  assignIfPresent(params, 'lat', filters.lat);
  assignIfPresent(params, 'lng', filters.lng);
  assignIfPresent(params, 'radiusKm', filters.radiusKm);
  assignIfPresent(params, 'bedrooms', filters.bedrooms);
  assignIfPresent(params, 'beds', filters.beds);
  assignIfPresent(params, 'bathrooms', filters.bathrooms);
  assignIfPresent(params, 'propertyTypes', filters.propertyTypes.length ? filters.propertyTypes.join(',') : null);
  assignIfPresent(params, 'amenityIds', filters.amenityIds.length ? filters.amenityIds.join(',') : null);
  assignIfTrue(params, 'instantBook', filters.instantBook);
  assignIfPresent(params, 'swLat', filters.swLat);
  assignIfPresent(params, 'swLng', filters.swLng);
  assignIfPresent(params, 'neLat', filters.neLat);
  assignIfPresent(params, 'neLng', filters.neLng);
  assignIfDifferent(params, 'sort', filters.sort, DEFAULT_SEARCH_FILTERS.sort);
  assignIfDifferent(params, 'page', filters.page, DEFAULT_SEARCH_FILTERS.page);
  assignIfDifferent(params, 'size', filters.size, DEFAULT_SEARCH_FILTERS.size);

  return params;
}

function shouldResetPage(patch: Partial<SearchFilters>): boolean {
  return Object.keys(patch).some((key) => !['page', 'size', 'sort'].includes(key));
}

function areFiltersEqual(left: SearchFilters, right: SearchFilters): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}

function areQueryParamsEqual(left: Params, right: Params): boolean {
  return canonicalizeParams(left) === canonicalizeParams(right);
}

function canonicalizeParams(params: Params): string {
  return Object.keys(params)
    .sort()
    .map((key) => `${key}=${String(params[key])}`)
    .join('&');
}

function assignIfPresent(params: Params, key: string, value: string | number | null): void {
  if (value !== null) {
    params[key] = String(value);
  }
}

function assignIfDifferent(
  params: Params,
  key: string,
  value: string | number,
  defaultValue: string | number
): void {
  if (value !== defaultValue) {
    params[key] = String(value);
  }
}

function assignIfTrue(params: Params, key: string, value: boolean): void {
  if (value) {
    params[key] = 'true';
  }
}

function emptyToNull(value: string): string | null {
  return value.trim() ? value.trim() : null;
}

function readString(value: unknown): string | null {
  const raw = lastValue(value);
  if (typeof raw !== 'string') {
    return null;
  }

  const trimmed = raw.trim();
  return trimmed ? trimmed : null;
}

function readStringArray(value: unknown): string[] {
  const rawValues = Array.isArray(value) ? value : [value];
  return [...new Set(rawValues
    .flatMap((entry) => typeof entry === 'string' ? entry.split(',') : [])
    .map((entry) => entry.trim())
    .filter((entry) => entry.length > 0))]
    .sort((left, right) => left.localeCompare(right));
}

function readDateString(value: unknown): string | null {
  const raw = readString(value);
  return raw && /^\d{4}-\d{2}-\d{2}$/.test(raw) ? raw : null;
}

function readBoolean(value: unknown): boolean {
  const raw = readString(value);
  return raw === 'true' || raw === '1';
}

function readSort(value: unknown): SearchSort {
  const raw = readString(value);
  return raw === 'priceAsc' || raw === 'priceDesc' || raw === 'newest' || raw === 'ratingDesc' || raw === 'distance'
    ? raw
    : DEFAULT_SEARCH_FILTERS.sort;
}

function readNumber(value: unknown): number | null {
  const raw = lastValue(value);
  if (typeof raw === 'number' && Number.isFinite(raw)) {
    return raw;
  }

  if (typeof raw !== 'string') {
    return null;
  }

  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
}

function readPositiveNumber(value: unknown): number | null {
  const parsed = readNumber(value);
  return parsed !== null && parsed > 0 ? parsed : null;
}

function readNonNegativeNumber(value: unknown): number | null {
  const parsed = readNumber(value);
  return parsed !== null && parsed >= 0 ? parsed : null;
}

function readPositiveInteger(value: unknown): number | null {
  const parsed = readNumber(value);
  return parsed !== null && Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

function readNonNegativeInteger(value: unknown): number | null {
  const parsed = readNumber(value);
  return parsed !== null && Number.isInteger(parsed) && parsed >= 0 ? parsed : null;
}

function readLatitude(value: unknown): number | null {
  const parsed = readNumber(value);
  return parsed !== null && parsed >= -90 && parsed <= 90 ? parsed : null;
}

function readLongitude(value: unknown): number | null {
  const parsed = readNumber(value);
  return parsed !== null && parsed >= -180 && parsed <= 180 ? parsed : null;
}

function lastValue(value: unknown): unknown {
  return Array.isArray(value) ? value[value.length - 1] : value;
}

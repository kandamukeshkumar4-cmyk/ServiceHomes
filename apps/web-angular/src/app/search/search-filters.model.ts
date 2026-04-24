export type SearchSort = 'relevance' | 'priceAsc' | 'priceDesc' | 'newest' | 'ratingDesc' | 'distance';

export interface SearchFilters {
  listingId: string | null;
  locationQuery: string;
  checkIn: string | null;
  checkOut: string | null;
  guests: number;
  minPrice: number | null;
  maxPrice: number | null;
  lat: number | null;
  lng: number | null;
  radiusKm: number | null;
  bedrooms: number | null;
  beds: number | null;
  bathrooms: number | null;
  propertyTypes: string[];
  amenityIds: string[];
  instantBook: boolean;
  swLat: number | null;
  swLng: number | null;
  neLat: number | null;
  neLng: number | null;
  sort: SearchSort;
  page: number;
  size: number;
}

export const DEFAULT_SEARCH_FILTERS: SearchFilters = {
  listingId: null,
  locationQuery: '',
  checkIn: null,
  checkOut: null,
  guests: 1,
  minPrice: null,
  maxPrice: null,
  lat: null,
  lng: null,
  radiusKm: null,
  bedrooms: null,
  beds: null,
  bathrooms: null,
  propertyTypes: [],
  amenityIds: [],
  instantBook: false,
  swLat: null,
  swLng: null,
  neLat: null,
  neLng: null,
  sort: 'relevance',
  page: 0,
  size: 20
};

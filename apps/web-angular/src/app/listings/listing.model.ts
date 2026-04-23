export interface Listing {
  id: string;
  hostId: string;
  title: string;
  description: string;
  category: ListingCategory;
  propertyType: string;
  maxGuests: number;
  bedrooms: number;
  beds: number;
  bathrooms: number;
  nightlyPrice: number;
  cleaningFee?: number;
  serviceFee?: number;
  status: 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED';
  createdAt: string;
  updatedAt: string;
  publishedAt?: string;
  averageRating?: number | null;
  reviewCount?: number;
  cleanlinessRating?: number | null;
  accuracyRating?: number | null;
  communicationRating?: number | null;
  locationRating?: number | null;
  valueRating?: number | null;
  trustScore?: number;
  location: ListingLocation;
  policy: ListingPolicy;
  photos: ListingPhoto[];
  amenities: ListingAmenity[];
}

export interface ListingPage {
  content: Listing[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ListingCategory {
  id: string;
  name: string;
  icon: string;
}

export interface ListingLocation {
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode?: string;
  country: string;
  latitude?: number;
  longitude?: number;
}

export interface ListingPolicy {
  checkInTime?: string;
  checkOutTime?: string;
  minNights: number;
  maxNights?: number;
  cancellationPolicy: string;
  instantBook: boolean;
}

export interface ListingPhoto {
  id: string;
  url: string;
  orderIndex: number;
  isCover: boolean;
}

export interface ListingAmenity {
  id: string;
  name: string;
  icon: string;
  category: string;
}

export interface CreateListingPayload {
  title: string;
  description: string;
  categoryId: string;
  propertyType: string;
  maxGuests: number;
  bedrooms: number;
  beds: number;
  bathrooms: number;
  nightlyPrice: number;
  cleaningFee?: number;
  serviceFee?: number;
  location: ListingLocation;
  policy: ListingPolicy;
  amenityIds: string[];
}

export interface ListingCardDto {
  id: string;
  title: string;
  coverUrl: string;
  city: string;
  country: string;
  nightlyPrice: number;
  categoryName: string;
  latitude: number;
  longitude: number;
  maxGuests: number;
  bedrooms: number;
  beds: number;
  bathrooms: number;
}

export interface ListingCardViewModel extends ListingCardDto {
  distanceKm?: number | null;
  averageRating?: number | null;
  reviewCount?: number;
  trustScore?: number | null;
  isSaved?: boolean;
}

export interface ListingSearchResult extends ListingCardViewModel {
  reviewCount: number;
}

export interface ListingSearchPage {
  content: ListingSearchResult[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}

export type ListingAvailabilityRuleType = 'BLOCKED_DATE' | 'MIN_NIGHTS_OVERRIDE' | 'PRICE_OVERRIDE';

export interface ListingAvailabilityRule {
  id?: string;
  ruleType: ListingAvailabilityRuleType;
  startDate: string;
  endDate: string;
  value?: number | null;
}

export interface ListingAvailabilityResponse {
  listingId: string;
  baseNightlyPrice: number;
  defaultMinNights: number;
  rules: ListingAvailabilityRule[];
}

export interface ListingCalendarDay {
  date: string;
  blocked: boolean;
  minNights: number;
  nightlyPrice: number;
  minNightsOverride: boolean;
  priceOverride: boolean;
}

export interface ListingCalendarResponse {
  listingId: string;
  startDate: string;
  endDate: string;
  days: ListingCalendarDay[];
}

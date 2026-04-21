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
  location: ListingLocation;
  policy: ListingPolicy;
  photos: ListingPhoto[];
  amenities: ListingAmenity[];
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

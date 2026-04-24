export interface ListingSummary {
  id: string;
  title: string;
  thumbnail?: string | null;
  price: number;
  rating?: number | null;
}

export interface WishlistSummary {
  id: string;
  ownerId: string;
  title: string;
  description?: string | null;
  coverPhotoUrl?: string | null;
  isPublic: boolean;
  owner: boolean;
  collaboratorCount: number;
  itemCount: number;
  updatedAt: string;
}

export interface WishlistItem {
  id: string;
  listing: ListingSummary;
  note?: string | null;
  sortOrder: number;
  addedAt: string;
}

export interface WishlistDetail extends WishlistSummary {
  shareToken?: string | null;
  collaboratorIds: string[];
  items: WishlistItem[];
  totalItems: number;
  editable: boolean;
}

export interface SavedSearch {
  id: string;
  name: string;
  filters: Record<string, unknown>;
  locationQuery?: string | null;
  geoCenterLat?: number | null;
  geoCenterLng?: number | null;
  radiusKm?: number | null;
  notifyNewResults: boolean;
  resultCountSnapshot?: number | null;
  createdAt: string;
}

export interface SaveSearchPayload {
  name: string;
  filters: Record<string, unknown>;
  locationQuery?: string | null;
  geoCenterLat?: number | null;
  geoCenterLng?: number | null;
  radiusKm?: number | null;
  notifyNewResults: boolean;
}

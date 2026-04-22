export interface ReservationRecord {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCoverUrl: string | null;
  listingCity: string | null;
  listingCountry: string | null;
  guestId: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  totalNights: number;
  nightlyPrice: number;
  cleaningFee: number;
  serviceFee: number;
  totalAmount: number;
  status: string;
  hostDisplayName: string;
  guestDisplayName: string;
}

export interface ReservationPage {
  content: ReservationRecord[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ReservationQuote {
  totalNights: number;
  nightlyPrice: number;
  subtotal: number;
  cleaningFee: number;
  serviceFee: number;
  totalAmount: number;
}

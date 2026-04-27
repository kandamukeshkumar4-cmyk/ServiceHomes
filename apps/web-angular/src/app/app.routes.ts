import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', loadComponent: () => import('./home/home.component').then(m => m.HomeComponent) },
  { path: 'search', loadComponent: () => import('./search/search-results.component').then(m => m.SearchResultsComponent) },
  { path: 'saved', canActivate: [authGuard], loadComponent: () => import('./saved/saved-listings-page.component').then(m => m.SavedListingsPageComponent) },
  { path: 'wishlists', canActivate: [authGuard], loadComponent: () => import('./wishlists/wishlist-page.component').then(m => m.WishlistPageComponent) },
  { path: 'wishlists/share/:token', loadComponent: () => import('./wishlists/public-wishlist.component').then(m => m.PublicWishlistComponent) },
  { path: 'wishlists/:id', canActivate: [authGuard], loadComponent: () => import('./wishlists/wishlist-detail.component').then(m => m.WishlistDetailComponent) },
  { path: 'saved-searches', canActivate: [authGuard], loadComponent: () => import('./wishlists/saved-searches.component').then(m => m.SavedSearchesComponent) },
  { path: 'inbox', canActivate: [authGuard], loadComponent: () => import('./messaging/inbox.component').then(m => m.InboxComponent) },
  { path: 'reservations/:id/messages', canActivate: [authGuard], loadComponent: () => import('./messaging/message-thread.component').then(m => m.MessageThreadComponent) },
  { path: 'listings/:id', loadComponent: () => import('./listings/listing-detail.component').then(m => m.ListingDetailComponent) },
  { path: 'hosts/:hostId', loadComponent: () => import('./host/host-profile.component').then(m => m.HostProfileComponent) },
  { path: 'account', canActivate: [authGuard], loadComponent: () => import('./account/account.component').then(m => m.AccountComponent) },
  { path: 'trips', canActivate: [authGuard], loadComponent: () => import('./dashboards/guest-dashboard.component').then(m => m.GuestDashboardComponent) },
  { path: 'bookings', canActivate: [authGuard], loadComponent: () => import('./bookings/my-bookings.component').then(m => m.MyBookingsComponent) },
  { path: 'bookings/:id', canActivate: [authGuard], loadComponent: () => import('./bookings/booking-detail.component').then(m => m.BookingDetailComponent) },
  { path: 'host', canActivate: [authGuard], loadComponent: () => import('./dashboards/host-dashboard.component').then(m => m.HostDashboardComponent) },
  { path: 'host/accommodations', canActivate: [authGuard], loadComponent: () => import('./host/host-accommodations.component').then(m => m.HostAccommodationsComponent) },
  { path: 'host/reservations', canActivate: [authGuard], loadComponent: () => import('./host/host-reservations.component').then(m => m.HostReservationsComponent) },
  { path: 'listings/new', canActivate: [authGuard], loadComponent: () => import('./listings/listing-create.component').then(m => m.ListingCreateComponent) },
  { path: 'listings/:id/edit', canActivate: [authGuard], loadComponent: () => import('./listings/listing-edit.component').then(m => m.ListingEditComponent) },
  { path: 'listings/:id/availability', canActivate: [authGuard], loadComponent: () => import('./listings/listing-availability.component').then(m => m.ListingAvailabilityComponent) },
  { path: 'host/listings/:id/calendar', canActivate: [authGuard], loadComponent: () => import('./host/host-calendar.component').then(m => m.HostCalendarComponent) }
];

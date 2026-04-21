import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', loadComponent: () => import('./home/home.component').then(m => m.HomeComponent) },
  { path: 'listings/:id', loadComponent: () => import('./listings/listing-detail.component').then(m => m.ListingDetailComponent) },
  { path: 'account', canActivate: [authGuard], loadComponent: () => import('./account/account.component').then(m => m.AccountComponent) },
  { path: 'bookings', canActivate: [authGuard], loadComponent: () => import('./bookings/my-bookings.component').then(m => m.MyBookingsComponent) },
  { path: 'host/accommodations', canActivate: [authGuard], loadComponent: () => import('./host/host-accommodations.component').then(m => m.HostAccommodationsComponent) },
  { path: 'listings/new', canActivate: [authGuard], loadComponent: () => import('./listings/listing-create.component').then(m => m.ListingCreateComponent) },
  { path: 'listings/:id/edit', canActivate: [authGuard], loadComponent: () => import('./listings/listing-edit.component').then(m => m.ListingEditComponent) }
];

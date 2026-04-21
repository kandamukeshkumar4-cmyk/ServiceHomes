import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CategoryShellComponent } from '../shell/category-shell.component';
import { SearchBarComponent } from '../search/search-bar.component';
import { ListingCardDto } from '../listings/listing.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, CategoryShellComponent, SearchBarComponent],
  templateUrl: './home.component.html',
  styles: []
})
export class HomeComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);

  listings: ListingCardDto[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.loadListings(params);
    });
  }

  loadListings(params: Record<string, any>) {
    this.loading = true;
    this.error = null;
    let httpParams = new HttpParams();
    if (params['locationQuery']) httpParams = httpParams.set('locationQuery', params['locationQuery']);
    if (params['categoryId']) httpParams = httpParams.set('categoryId', params['categoryId']);
    if (params['checkIn']) httpParams = httpParams.set('checkIn', params['checkIn']);
    if (params['checkOut']) httpParams = httpParams.set('checkOut', params['checkOut']);
    if (params['guests']) httpParams = httpParams.set('guests', params['guests']);

    this.http.get<ListingCardDto[]>('/api/listings/search', { params: httpParams }).subscribe({
      next: data => {
        this.listings = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load listings. Please try again.';
        this.listings = [];
        this.loading = false;
      }
    });
  }
}

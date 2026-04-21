import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
  listings: ListingCardDto[] = [];

  ngOnInit() {
    this.http.get<ListingCardDto[]>('/api/listings/search').subscribe({
      next: data => this.listings = data,
      error: () => this.listings = []
    });
  }
}

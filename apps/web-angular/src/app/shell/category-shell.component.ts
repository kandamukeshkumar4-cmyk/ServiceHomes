import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ListingService } from '../listings/listing.service';
import { ListingCategory } from '../listings/listing.model';

@Component({
  selector: 'app-category-shell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="category-shell flex gap-3 overflow-x-auto py-3 px-4 surface-0 border-bottom-1 surface-border">
      <span class="category-chip px-3 py-2 border-round-lg text-700 text-sm font-medium cursor-pointer transition-colors"
            [ngClass]="{'surface-200 hover:surface-300': !selectedCategory, 'bg-primary text-white': selectedCategory === 'all'}"
            (click)="selectCategory('all')">
        All
      </span>
      <span *ngFor="let cat of categories"
            class="category-chip px-3 py-2 border-round-lg text-700 text-sm font-medium cursor-pointer transition-colors"
            [ngClass]="{'surface-200 hover:surface-300': selectedCategory !== cat.id, 'bg-primary text-white': selectedCategory === cat.id}"
            (click)="selectCategory(cat.id)">
        <i *ngIf="cat.icon" [class]="cat.icon" class="mr-1"></i>{{ cat.name }}
      </span>
    </div>
  `,
  styles: [`.category-shell { scrollbar-width: none; }`]
})
export class CategoryShellComponent implements OnInit {
  private listingService = inject(ListingService);
  private router = inject(Router);

  categories: ListingCategory[] = [];
  selectedCategory: string | null = 'all';

  ngOnInit() {
    this.listingService.getCategories().subscribe((cats: ListingCategory[]) => this.categories = cats);
  }

  selectCategory(id: string | null) {
    this.selectedCategory = id;
    const params: Record<string, string> = {};
    if (id && id !== 'all') params['categoryId'] = id;
    this.router.navigate(['/home'], { queryParams: params });
  }
}

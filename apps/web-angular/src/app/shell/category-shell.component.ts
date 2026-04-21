import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-category-shell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="category-shell flex gap-3 overflow-x-auto py-3 px-4 surface-0 border-bottom-1 surface-border">
      <span *ngFor="let cat of categories"
            class="category-chip px-3 py-2 border-round-lg surface-200 text-700 text-sm font-medium cursor-pointer hover:surface-300 transition-colors">
        {{ cat }}
      </span>
    </div>
  `,
  styles: [`.category-shell { scrollbar-width: none; }`]
})
export class CategoryShellComponent {
  categories = [
    'Trending', 'Beachfront', 'Cabins', 'Tiny homes', 'Amazing pools',
    'Farms', 'Treehouses', 'Camping', 'Castles', 'Boats', 'Arctic', 'Desert'
  ];
}

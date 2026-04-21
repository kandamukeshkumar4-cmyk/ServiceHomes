import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoryShellComponent } from '../shell/category-shell.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CategoryShellComponent],
  template: `
    <app-category-shell />
    <div class="p-4">
      <h1 class="text-3xl font-bold mb-2">Welcome to ServiceHomes</h1>
      <p class="text-600">Find your next stay, or host your space.</p>
    </div>
  `,
  styles: []
})
export class HomeComponent {}

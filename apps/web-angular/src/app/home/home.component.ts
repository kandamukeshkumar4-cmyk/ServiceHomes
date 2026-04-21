import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-4">
      <h1>ServiceHomes</h1>
      <p>Welcome to the home services platform.</p>
    </div>
  `,
  styles: []
})
export class HomeComponent {}

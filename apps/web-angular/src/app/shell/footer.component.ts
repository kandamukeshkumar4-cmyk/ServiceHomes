import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="surface-100 py-4 px-4 text-center text-600 text-sm mt-auto">
      <p> ServiceHomes. All rights reserved.</p>
    </footer>
  `,
  styles: []
})
export class FooterComponent {}

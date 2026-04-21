import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shell/navbar.component';
import { FooterComponent } from './shell/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  template: `
    <div class="flex flex-column min-h-screen">
      <app-navbar />
      <main class="flex-1">
        <router-outlet></router-outlet>
      </main>
      <app-footer />
    </div>
  `,
  styles: []
})
export class AppComponent {
  title = 'ServiceHomes';
}

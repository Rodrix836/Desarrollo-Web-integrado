import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
})
export class App {
  auth = inject(AuthService);
  constructor() {
    if (this.auth.token()) this.auth.refresh().subscribe({ error: () => this.auth.logout() });
  }
}

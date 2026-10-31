import { Component, inject } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class App {
  constructor() {
    // <mat-icon>name</mat-icon> renders with the Material Symbols font loaded in index.html.
    inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
  }
}

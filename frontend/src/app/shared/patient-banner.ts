import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Allergy, Patient } from '../core/models';

/** Patient identity strip shown on the chart and encounter screens (with allergy warning). */
@Component({
  selector: 'cc-patient-banner',
  imports: [MatIconModule, MatTooltipModule, RouterLink, DatePipe],
  template: `
    @let p = patient();
    <div class="banner">
      <div class="avatar">{{ p.firstName.charAt(0) }}{{ p.lastName.charAt(0) }}</div>
      <div class="who">
        <div class="name">
          @if (link()) { <a [routerLink]="['/patients', p.id]">{{ p.fullName }}</a> } @else { {{ p.fullName }} }
        </div>
        <div class="meta">
          <span class="mono">{{ p.mrn }}</span>
          <span>{{ p.age }} yrs &middot; {{ p.gender === 'MALE' ? 'Male' : p.gender === 'FEMALE' ? 'Female' : 'Other' }}</span>
          @if (p.dateOfBirth) { <span>DOB {{ p.dateOfBirth | date: 'dd MMM yyyy' }}</span> }
          @if (p.bloodGroup) { <span class="chip danger">{{ p.bloodGroup }}</span> }
          @if (p.phone) { <span><mat-icon inline>call</mat-icon> {{ p.phone }}</span> }
          @if (p.primaryProviderName) { <span><mat-icon inline>stethoscope</mat-icon> {{ p.primaryProviderName }}</span> }
        </div>
      </div>
      @if (allergies(); as list) {
        <div class="allergies" [class.none]="!list.length">
          <mat-icon>{{ list.length ? 'warning' : 'check_circle' }}</mat-icon>
          @if (list.length) {
            <div>
              <div class="al-title">Allergies</div>
              @for (a of list; track a.id) {
                <span class="chip" [class.danger]="a.severity === 'SEVERE'" [class.warning]="a.severity !== 'SEVERE'"
                      [matTooltip]="(a.reaction || '') + ' (' + a.severity + ')'">{{ a.allergen }}</span>
              }
            </div>
          } @else {
            <span>No known allergies</span>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .banner { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; padding: 16px 20px;
      background: linear-gradient(120deg, #0b5cad, #0e7fa0); color: #fff; border-radius: var(--cc-radius); box-shadow: var(--cc-shadow); }
    .avatar { width: 54px; height: 54px; border-radius: 50%; background: rgba(255,255,255,.18); display: grid; place-items: center;
      font-weight: 700; font-size: 1.2rem; flex: none; }
    .who { flex: 1 1 280px; min-width: 0; }
    .name { font-size: 1.35rem; font-weight: 700; a { color: #fff; text-decoration: none; } }
    .meta { display: flex; flex-wrap: wrap; gap: 6px 16px; opacity: .92; font-size: .9rem; margin-top: 4px; align-items: center; }
    .allergies { display: flex; gap: 10px; align-items: flex-start; background: #fff; color: #8e1c1c; border-radius: 10px; padding: 10px 14px; max-width: 460px;
      &.none { color: var(--cc-success); align-items: center; }
      .al-title { font-weight: 700; font-size: .8rem; text-transform: uppercase; letter-spacing: .05em; margin-bottom: 4px; }
      .chip { margin: 2px 4px 2px 0; }
    }
  `,
})
export class PatientBanner {
  readonly patient = input.required<Patient>();
  readonly allergies = input<Allergy[] | null>(null);
  readonly link = input(false);
}

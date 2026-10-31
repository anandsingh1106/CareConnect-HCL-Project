import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { DEMO_ACCOUNTS } from './login';

@Component({
  selector: 'cc-landing',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  readonly auth = inject(AuthService);
  readonly accounts = DEMO_ACCOUNTS;
  readonly year = new Date().getFullYear();

  readonly features = [
    { icon: 'folder_shared', title: 'Complete patient records', text: 'Auto-generated MRN, demographics, allergies, problem list, immunizations, family history and scanned documents in one chart.' },
    { icon: 'edit_note', title: 'SOAP documentation', text: 'Template-based progress notes, ICD-10 coded diagnoses, e-signature with locking and auditable addenda.' },
    { icon: 'monitor_heart', title: 'Vitals & trends', text: 'Record BP, pulse, SpO2, temperature and weight with automatic BMI and interactive trend charts.' },
    { icon: 'shield_with_heart', title: 'CPOE with decision support', text: 'Lab, imaging, medication and referral orders with drug-allergy, drug-drug and duplicate-therapy alerts.' },
    { icon: 'local_pharmacy', title: 'e-Prescribing & pharmacy', text: 'Pharmacist verification and dispensing queues, stock deduction, low-stock alerts and full inventory.' },
    { icon: 'medication', title: 'Medication administration', text: 'Nurses document every dose on the MAR - given, held or refused - with reasons and timestamps.' },
    { icon: 'calendar_month', title: 'Smart scheduling', text: 'Provider slots, double-booking prevention, check-in and online booking from the patient portal.' },
    { icon: 'favorite', title: 'Patient portal', text: 'Patients see their medicines, lab results and visit summaries, download PDFs and message their doctor.' },
  ];

  readonly roles = [
    { icon: 'stethoscope', name: 'Doctor', text: 'Charts, notes, orders, e-prescriptions' },
    { icon: 'vaccines', name: 'Nurse', text: 'Vitals, MAR, results entry' },
    { icon: 'local_pharmacy', name: 'Pharmacist', text: 'Verify, dispense, inventory' },
    { icon: 'support_agent', name: 'Receptionist', text: 'Registration and appointments' },
    { icon: 'admin_panel_settings', name: 'Admin', text: 'Users, master data, audit' },
    { icon: 'person', name: 'Patient', text: 'Portal, bookings, messages' },
  ];

  readonly flow = [
    { icon: 'how_to_reg', title: 'Register & book', text: 'Reception registers the patient and books a slot.' },
    { icon: 'clinical_notes', title: 'Consult', text: 'Doctor records vitals, SOAP note and diagnosis.' },
    { icon: 'fact_check', title: 'Order safely', text: 'CDS checks allergies and interactions before ordering.' },
    { icon: 'local_pharmacy', title: 'Verify & dispense', text: 'Pharmacist verifies the e-Rx and dispenses stock.' },
    { icon: 'download', title: 'Stay informed', text: 'Patient views results and downloads the visit PDF.' },
  ];
}

import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Allergy, EncounterDetail, Icd10, Note, NoteTemplate, Order, Patient } from '../../core/models';
import { Icd10Picker } from '../../shared/icd10-picker';
import { PatientBanner } from '../../shared/patient-banner';
import { LabelPipe, StatusChip } from '../../shared/status-chip';
import { Ui } from '../../shared/ui.service';
import { VitalsDialog } from '../../shared/vitals-dialog';
import { OrderActions } from '../orders/order-actions';
import { OrderDialog } from '../orders/order-dialog';

@Component({
  selector: 'cc-encounter-detail',
  imports: [
    ReactiveFormsModule, FormsModule, RouterLink, DatePipe, LabelPipe, StatusChip, PatientBanner, Icd10Picker, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressBarModule, MatMenuModule, MatTooltipModule,
  ],
  providers: [OrderActions],
  templateUrl: './encounter-detail.html',
  styleUrl: './encounter-detail.scss',
})
export class EncounterDetailPage implements OnInit {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  readonly orderActions = inject(OrderActions);

  readonly id = input.required<string>();
  readonly detail = signal<EncounterDetail | null>(null);
  readonly patient = signal<Patient | null>(null);
  readonly allergies = signal<Allergy[] | null>(null);
  readonly orders = signal<Order[]>([]);
  readonly templates = signal<NoteTemplate[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  /** Note being edited (null + editorOpen = new note). */
  readonly editing = signal<Note | null>(null);
  readonly editorOpen = signal(false);
  readonly addendumFor = signal<Note | null>(null);

  readonly isDoctor = this.auth.hasRole('DOCTOR');
  readonly canVitals = this.auth.hasRole('DOCTOR', 'NURSE');
  readonly isOpen = computed(() => this.detail()?.encounter.status === 'OPEN');
  readonly canDocument = computed(() => this.isDoctor && this.isOpen());

  /** Progress notes with their addenda grouped underneath. */
  readonly noteTree = computed(() => {
    const notes = this.detail()?.notes ?? [];
    return notes.filter(n => n.noteType === 'PROGRESS').map(n => ({
      note: n,
      addenda: notes.filter(a => a.noteType === 'ADDENDUM' && a.parentNoteId === n.id),
    }));
  });
  readonly myDraft = computed(() => (this.detail()?.notes ?? []).find(n => n.status === 'DRAFT' && n.authorId === this.auth.user()?.id) ?? null);

  readonly noteForm = inject(FormBuilder).nonNullable.group({
    templateCode: [''],
    subjective: [''],
    objective: [''],
    assessment: [''],
    plan: [''],
  });
  readonly addendumText = inject(FormBuilder).nonNullable.control('');
  dxType: 'PRIMARY' | 'SECONDARY' = 'SECONDARY';
  dxCode: Icd10 | null = null;

  ngOnInit(): void {
    this.load();
    if (this.isDoctor) this.api.noteTemplates().subscribe(t => this.templates.set(t));
  }

  load(): void {
    const id = +this.id();
    this.api.encounter(id).subscribe({
      next: d => {
        this.detail.set(d);
        this.loading.set(false);
        if (!d.diagnoses.some(x => x.diagnosisType === 'PRIMARY')) this.dxType = 'PRIMARY';
        const pid = d.encounter.patientId;
        if (!this.patient()) {
          forkJoin({ p: this.api.patient(pid), a: this.api.allergies(pid) }).subscribe(({ p, a }) => {
            this.patient.set(p);
            this.allergies.set(a.filter(x => x.status === 'ACTIVE'));
          });
        }
      },
      error: err => {
        this.loading.set(false);
        this.ui.error(err);
      },
    });
    this.api.encounterOrders(id).subscribe(o => this.orders.set(o));
  }

  // ---------- notes ----------
  newNote(): void {
    const draft = this.myDraft();
    if (draft) {
      this.edit(draft);
      return;
    }
    this.editing.set(null);
    this.noteForm.reset();
    this.editorOpen.set(true);
  }

  edit(note: Note): void {
    this.editing.set(note);
    this.noteForm.setValue({
      templateCode: note.templateCode ?? '',
      subjective: note.subjective ?? '',
      objective: note.objective ?? '',
      assessment: note.assessment ?? '',
      plan: note.plan ?? '',
    });
    this.editorOpen.set(true);
  }

  async applyTemplate(code: string): Promise<void> {
    const t = this.templates().find(x => x.code === code);
    if (!t) return;
    const v = this.noteForm.getRawValue();
    const hasText = !!(v.subjective || v.objective || v.assessment || v.plan);
    if (hasText && !(await this.ui.confirm({ title: 'Apply template?', message: `Replace the current text with the "${t.name}" template?`, confirmText: 'Replace' }))) {
      return;
    }
    this.noteForm.patchValue({ subjective: t.subjective ?? '', objective: t.objective ?? '', assessment: t.assessment ?? '', plan: t.plan ?? '' });
  }

  saveNote(signAfter = false): void {
    const v = this.noteForm.getRawValue();
    if (!(v.subjective.trim() || v.objective.trim() || v.assessment.trim() || v.plan.trim())) {
      this.ui.error(new Error('Write at least one SOAP section.'));
      return;
    }
    const body = { ...v, templateCode: v.templateCode || null, version: this.editing()?.version };
    const existing = this.editing();
    this.saving.set(true);
    const req = existing ? this.api.updateNote(existing.id, body) : this.api.createNote(+this.id(), body);
    req.subscribe({
      next: note => {
        this.saving.set(false);
        if (signAfter) {
          this.sign(note);
        } else {
          this.ui.success('Draft saved.');
          this.editing.set(note);
          this.load();
        }
      },
      error: err => {
        this.saving.set(false);
        this.ui.error(err);
      },
    });
  }

  async sign(note: Note): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Sign this note?',
      message: 'Signing locks the note. Later corrections can only be added as addenda.',
      confirmText: 'Sign note',
    });
    if (!ok) {
      this.load();
      return;
    }
    this.api.signNote(note.id).subscribe({
      next: () => {
        this.ui.success('Note signed and locked.');
        this.editorOpen.set(false);
        this.editing.set(null);
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }

  cancelEdit(): void {
    this.editorOpen.set(false);
    this.editing.set(null);
  }

  startAddendum(note: Note): void {
    this.addendumFor.set(note);
    this.addendumText.setValue('');
  }

  saveAddendum(): void {
    const parent = this.addendumFor();
    const text = this.addendumText.value.trim();
    if (!parent || !text) {
      this.ui.error(new Error('The addendum is empty.'));
      return;
    }
    this.api.addendum(parent.id, { plan: text }).subscribe({
      next: () => {
        this.ui.success('Addendum signed and added.');
        this.addendumFor.set(null);
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }

  // ---------- diagnoses ----------
  addDiagnosis(picker: Icd10Picker): void {
    if (!this.dxCode) {
      this.ui.error(new Error('Pick an ICD-10 code from the list.'));
      return;
    }
    this.api.addDiagnosis(+this.id(), { icd10Code: this.dxCode.code, diagnosisType: this.dxType }).subscribe({
      next: () => {
        this.ui.success(`${this.dxCode?.code} added.`);
        this.dxCode = null;
        picker.clear();
        this.dxType = 'SECONDARY';
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }

  async removeDiagnosis(id: number, code: string): Promise<void> {
    if (!(await this.ui.confirm({ title: 'Remove diagnosis?', message: `${code} will be removed from this encounter.`, confirmText: 'Remove', danger: true }))) return;
    this.api.removeDiagnosis(id).subscribe({ next: () => this.load(), error: err => this.ui.error(err) });
  }

  // ---------- vitals / orders / close ----------
  recordVitals(): void {
    const e = this.detail()!.encounter;
    this.dialog.open(VitalsDialog, { data: { patientId: e.patientId, patientName: e.patientName, encounterId: e.id }, width: '720px', maxWidth: '96vw' })
      .afterClosed().subscribe(v => v && this.load());
  }

  newOrder(): void {
    const e = this.detail()!.encounter;
    this.dialog.open(OrderDialog, { data: { patientId: e.patientId, patientName: e.patientName, encounterId: e.id }, maxWidth: '96vw' })
      .afterClosed().subscribe(o => o && this.load());
  }

  readonly reload = () => this.load();

  soapText(n: Note): string {
    return [n.subjective, n.objective, n.assessment, n.plan].filter(x => !!x).join('\n');
  }

  async close(): Promise<void> {
    const ok = await this.ui.confirm({
      title: 'Close encounter?',
      message: 'All notes must be signed. After closing, only addenda can be added and no new orders can be placed.',
      confirmText: 'Close encounter',
    });
    if (!ok) return;
    this.api.closeEncounter(+this.id()).subscribe({
      next: () => {
        this.ui.success('Encounter closed.');
        this.load();
      },
      error: err => this.ui.error(err),
    });
  }
}

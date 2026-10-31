import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Conversation, Message, Provider } from '../../core/models';
import { Ui } from '../../shared/ui.service';

/** Secure messaging between patients and their doctors. Patients can start a conversation; doctors reply. */
@Component({
  selector: 'cc-messages',
  imports: [FormsModule, DatePipe, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Messages</h1><p>{{ isPatient ? 'Ask your doctor a question. For emergencies call the hospital.' : 'Secure messages from your patients.' }}</p></div>
        @if (isPatient) { <button mat-flat-button (click)="compose()"><mat-icon>edit</mat-icon>New message</button> }
      </div>
      <div class="layout card">
        <aside class="list">
          @for (c of conversations(); track c.counterpartId) {
            <button class="conv" [class.sel]="active()?.id === c.counterpartId" (click)="open(c.counterpartId, c.counterpartName)">
              <div class="av">{{ c.counterpartName.charAt(0) }}</div>
              <div class="cmain">
                <div class="row"><strong>{{ c.counterpartName }}</strong><span class="muted small">{{ c.lastAt | date: 'dd MMM' }}</span></div>
                <div class="small">{{ c.lastSubject }}</div>
                <div class="muted small ell">{{ c.lastPreview }}</div>
              </div>
              @if (c.unread) { <span class="badge">{{ c.unread }}</span> }
            </button>
          } @empty { <div class="empty"><mat-icon>forum</mat-icon>No conversations yet.</div> }
        </aside>

        <section class="thread">
          @if (active(); as a) {
            <header class="th"><strong>{{ a.name }}</strong></header>
            <div class="msgs">
              @for (m of thread(); track m.id) {
                <div class="msg" [class.mine]="m.mine">
                  <div class="bubble">
                    <div class="subj">{{ m.subject }}</div>
                    <div class="pre">{{ m.body }}</div>
                    <div class="meta">{{ m.senderName }} &middot; {{ m.createdAt | date: 'dd MMM, HH:mm' }}@if (m.mine && m.readAt) { &middot; read }</div>
                  </div>
                </div>
              }
            </div>
            <div class="reply">
              <mat-form-field subscriptSizing="dynamic" class="subject"><mat-label>Subject</mat-label><input matInput [(ngModel)]="subject" maxlength="200" /></mat-form-field>
              <mat-form-field subscriptSizing="dynamic" class="body"><mat-label>Message</mat-label><textarea matInput rows="2" [(ngModel)]="body" maxlength="4000"></textarea></mat-form-field>
              <button mat-flat-button (click)="send()" [disabled]="sending()"><mat-icon>send</mat-icon>Send</button>
            </div>
          } @else if (composing()) {
            <div class="compose">
              <h3>New message</h3>
              <mat-form-field><mat-label>Doctor</mat-label>
                <mat-select [(ngModel)]="recipientId">@for (p of providers(); track p.id) { <mat-option [value]="p.id">{{ p.fullName }} &middot; {{ p.specialty }}</mat-option> }</mat-select></mat-form-field>
              <mat-form-field><mat-label>Subject</mat-label><input matInput [(ngModel)]="subject" maxlength="200" /></mat-form-field>
              <mat-form-field><mat-label>Message</mat-label><textarea matInput rows="6" [(ngModel)]="body" maxlength="4000"></textarea></mat-form-field>
              <div class="actions"><button mat-button (click)="composing.set(false)">Cancel</button><button mat-flat-button (click)="send()"><mat-icon>send</mat-icon>Send</button></div>
            </div>
          } @else {
            <div class="empty"><mat-icon>mark_chat_read</mat-icon>Select a conversation.</div>
          }
        </section>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 320px 1fr; padding: 0; min-height: 60vh; overflow: hidden; }
    .list { border-right: 1px solid var(--cc-border); overflow-y: auto; max-height: 72vh; }
    .conv { display: flex; gap: 10px; width: 100%; padding: 14px; border: none; border-bottom: 1px solid var(--cc-border); background: none; text-align: left; cursor: pointer; font: inherit; color: inherit;
      &:hover { background: #f5f9fd; } &.sel { background: #e8f1fc; } }
    .av { width: 38px; height: 38px; border-radius: 50%; background: #e3eefa; color: var(--cc-primary); display: grid; place-items: center; font-weight: 700; flex: none; }
    .cmain { flex: 1; min-width: 0; } .row { display: flex; justify-content: space-between; gap: 6px; }
    .ell { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .badge { background: var(--cc-danger); color: #fff; border-radius: 999px; padding: 0 8px; height: 20px; font-size: .75rem; display: grid; place-items: center; }
    .thread { display: flex; flex-direction: column; min-width: 0; }
    .th { padding: 14px 18px; border-bottom: 1px solid var(--cc-border); }
    .msgs { flex: 1; overflow-y: auto; padding: 18px; display: flex; flex-direction: column; gap: 10px; max-height: 52vh; background: #f8fafc; }
    .msg { display: flex; } .msg.mine { justify-content: flex-end; }
    .bubble { max-width: 75%; background: #fff; border: 1px solid var(--cc-border); border-radius: 14px 14px 14px 4px; padding: 10px 14px; }
    .mine .bubble { background: #e3eefa; border-color: #c4dbf4; border-radius: 14px 14px 4px 14px; }
    .subj { font-weight: 700; font-size: .85rem; margin-bottom: 4px; } .meta { font-size: .72rem; color: var(--cc-muted); margin-top: 6px; }
    .reply { display: flex; gap: 10px; align-items: center; padding: 12px; border-top: 1px solid var(--cc-border); flex-wrap: wrap;
      .subject { width: 220px; } .body { flex: 1; min-width: 200px; } }
    .compose { padding: 20px; display: grid; }
    @media (max-width: 800px) { .layout { grid-template-columns: 1fr; } .list { max-height: 30vh; border-right: none; } }
  `,
})
export class Messages {
  private readonly api = inject(Api);
  private readonly ui = inject(Ui);
  readonly isPatient = inject(AuthService).hasRole('PATIENT');
  readonly conversations = signal<Conversation[]>([]);
  readonly thread = signal<Message[]>([]);
  readonly active = signal<{ id: number; name: string } | null>(null);
  readonly composing = signal(false);
  readonly providers = signal<Provider[]>([]);
  readonly sending = signal(false);
  readonly lastSubject = computed(() => this.thread().at(-1)?.subject ?? '');
  recipientId: number | null = null;
  subject = '';
  body = '';

  constructor() {
    this.loadConversations();
  }

  loadConversations(): void {
    this.api.conversations().subscribe({ next: c => this.conversations.set(c), error: err => this.ui.error(err) });
  }

  open(id: number, name: string): void {
    this.composing.set(false);
    this.active.set({ id, name });
    this.api.thread(id).subscribe(t => {
      this.thread.set(t);
      const last = this.lastSubject();
      this.subject = last ? (last.startsWith('Re:') ? last : `Re: ${last}`) : '';
      this.body = '';
      this.conversations.update(list => list.map(c => (c.counterpartId === id ? { ...c, unread: 0 } : c)));
    });
  }

  compose(): void {
    this.active.set(null);
    this.composing.set(true);
    this.subject = '';
    this.body = '';
    if (!this.providers().length) this.api.providers().subscribe(p => this.providers.set(p));
  }

  send(): void {
    const recipient = this.active()?.id ?? this.recipientId;
    if (!recipient || !this.subject.trim() || !this.body.trim()) {
      this.ui.error(new Error('Choose a recipient and fill in the subject and message.'));
      return;
    }
    this.sending.set(true);
    this.api.sendMessage({ recipientId: recipient, subject: this.subject.trim(), body: this.body.trim() }).subscribe({
      next: () => {
        this.sending.set(false);
        this.ui.success('Message sent.');
        const name = this.active()?.name ?? this.providers().find(p => p.id === recipient)?.fullName ?? '';
        this.loadConversations();
        this.open(recipient, name);
      },
      error: err => {
        this.sending.set(false);
        this.ui.error(err);
      },
    });
  }
}

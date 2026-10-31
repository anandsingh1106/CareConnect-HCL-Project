import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

/** Turns an HTTP error into one readable sentence (field errors included). */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the CareConnect server. Is the backend running on port 8090?';
    }
    const body = err.error as Partial<ApiError> | null;
    if (body && typeof body === 'object' && 'message' in body) {
      const fields = body.fieldErrors?.map(f => `${label(f.field)}: ${f.message}`).join('; ');
      return fields ? `${body.message} ${fields}` : (body.message ?? err.message);
    }
    if (err.status === 403) return 'You do not have permission for this action.';
    return err.message;
  }
  return err instanceof Error ? err.message : 'Something went wrong.';
}

export function apiError(err: unknown): Partial<ApiError> | null {
  return err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' ? (err.error as ApiError) : null;
}

/** "PENDING_VERIFICATION" -> "Pending verification", "firstName" -> "First name". */
export function label(value: string | null | undefined): string {
  if (!value) return '';
  const spaced = value.includes('_') ? value.toLowerCase().replace(/_/g, ' ') : value.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

/** Local date as yyyy-MM-dd (what Spring expects for LocalDate). */
export function isoDate(d: Date | string | null | undefined): string | null {
  if (!d) return null;
  if (typeof d === 'string') return d.slice(0, 10);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Local date-time as yyyy-MM-ddTHH:mm:ss (LocalDateTime, no zone). */
export function isoDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${isoDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export function addDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + days);
  return copy;
}

/** Parses "yyyy-MM-dd" as a local date (new Date("yyyy-MM-dd") would be UTC). */
export function parseDate(s: string | null | undefined): Date | null {
  if (!s) return null;
  const [y, m, d] = s.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

/** Removes empty strings so optional fields are sent as null. */
export function clean<T extends object>(value: T): T {
  const out: Record<string, unknown> = {};
  Object.entries(value).forEach(([k, v]) => {
    out[k] = v === '' ? null : v instanceof Date ? isoDate(v) : v;
  });
  return out as T;
}

export function fileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

const TONES: Record<string, string> = {
  // generic
  ACTIVE: 'success', INACTIVE: '', RESOLVED: '',
  // encounters / notes
  OPEN: 'info', CLOSED: '', CANCELLED: '', DRAFT: 'warning', SIGNED: 'success',
  PROGRESS: 'primary', ADDENDUM: 'info', PRIMARY: 'primary', SECONDARY: '',
  // orders
  PLACED: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success',
  ROUTINE: '', URGENT: 'warning', STAT: 'danger',
  // results
  NORMAL: 'success', LOW: 'warning', HIGH: 'warning', CRITICAL_LOW: 'danger', CRITICAL_HIGH: 'danger', ABNORMAL: 'danger',
  PRELIMINARY: 'warning', FINAL: 'success', CORRECTED: 'info',
  // prescriptions
  PENDING_VERIFICATION: 'warning', VERIFIED: 'info', DISPENSED: 'success', REJECTED: 'danger', DISCONTINUED: '',
  GIVEN: 'success', HELD: 'warning', REFUSED: 'danger', MISSED: 'danger',
  // alerts / allergies / interactions
  INFO: 'info', WARNING: 'warning', CRITICAL: 'danger',
  MILD: 'info', MODERATE: 'warning', SEVERE: 'danger',
  MINOR: 'info', MAJOR: 'danger', CONTRAINDICATED: 'danger',
  // scheduling
  AVAILABLE: 'success', BOOKED: 'primary', BLOCKED: '', CHECKED_IN: 'warning', NO_SHOW: 'danger',
  PORTAL: 'info', STAFF: '', TELEHEALTH: 'info', IN_PERSON: '',
  // audit
  CREATE: 'success', READ: 'info', UPDATE: 'warning', DELETE: 'danger', LOGIN: 'primary', LOGIN_FAILED: 'danger', LOGOUT: '',
  // roles
  ADMIN: 'danger', DOCTOR: 'primary', NURSE: 'info', PHARMACIST: 'success', RECEPTIONIST: 'warning', PATIENT: '',
};

export function tone(value: string | null | undefined): string {
  return value ? (TONES[value] ?? '') : '';
}

export const FREQUENCIES = ['OD', 'BD', 'TDS', 'QID', 'HS', 'Q4H', 'Q6H', 'Q8H', 'SOS', 'STAT', 'WEEKLY'];

/** Doses per day for a frequency code (used to suggest a quantity). */
export function dosesPerDay(freq: string): number {
  const map: Record<string, number> = { OD: 1, HS: 1, BD: 2, TDS: 3, QID: 4, Q4H: 6, Q6H: 4, Q8H: 3, SOS: 1, STAT: 1, WEEKLY: 1 / 7 };
  return map[freq?.toUpperCase()] ?? 1;
}

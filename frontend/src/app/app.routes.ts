import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards';
import { Role } from './core/models';

const roles = (...r: Role[]) => ({ roles: r });
const CLINICAL: Role[] = ['DOCTOR', 'NURSE', 'PHARMACIST'];

export const routes: Routes = [
  { path: '', pathMatch: 'full', title: 'CareConnect', loadComponent: () => import('./pages/public/landing').then(m => m.Landing) },
  { path: 'login', title: 'Sign in | CareConnect', canActivate: [guestGuard], loadComponent: () => import('./pages/public/login').then(m => m.Login) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    children: [
      { path: 'dashboard', title: 'Dashboard', loadComponent: () => import('./pages/dashboard').then(m => m.DashboardPage) },

      // patients
      { path: 'patients', title: 'Patients', canActivate: [authGuard], data: roles('DOCTOR', 'NURSE', 'PHARMACIST', 'RECEPTIONIST', 'ADMIN'),
        loadComponent: () => import('./pages/patients/patient-list').then(m => m.PatientList) },
      { path: 'patients/new', title: 'Register patient', canActivate: [authGuard], data: roles('RECEPTIONIST', 'DOCTOR'),
        loadComponent: () => import('./pages/patients/patient-form').then(m => m.PatientForm) },
      { path: 'patients/:id/edit', title: 'Edit patient', canActivate: [authGuard], data: roles('RECEPTIONIST', 'DOCTOR'),
        loadComponent: () => import('./pages/patients/patient-form').then(m => m.PatientForm) },
      { path: 'patients/:id', title: 'Patient chart', canActivate: [authGuard], data: roles('DOCTOR', 'NURSE', 'PHARMACIST', 'RECEPTIONIST'),
        loadComponent: () => import('./pages/patients/patient-chart').then(m => m.PatientChart) },

      // clinical
      { path: 'encounters', title: 'Encounters', canActivate: [authGuard], data: roles('DOCTOR', 'NURSE'),
        loadComponent: () => import('./pages/clinical/encounter-list').then(m => m.EncounterList) },
      { path: 'encounters/:id', title: 'Encounter', canActivate: [authGuard], data: { roles: CLINICAL },
        loadComponent: () => import('./pages/clinical/encounter-detail').then(m => m.EncounterDetailPage) },
      { path: 'orders', title: 'Orders', canActivate: [authGuard], data: { roles: CLINICAL },
        loadComponent: () => import('./pages/orders/order-list').then(m => m.OrderList) },

      // pharmacy & nursing
      { path: 'pharmacy', title: 'Pharmacy', canActivate: [authGuard], data: roles('PHARMACIST'),
        loadComponent: () => import('./pages/pharmacy/pharmacy-queue').then(m => m.PharmacyQueue) },
      { path: 'pharmacy/inventory', title: 'Drug inventory', canActivate: [authGuard], data: roles('PHARMACIST', 'ADMIN'),
        loadComponent: () => import('./pages/pharmacy/inventory').then(m => m.Inventory) },
      { path: 'nursing', title: 'Nursing - MAR', canActivate: [authGuard], data: roles('NURSE', 'DOCTOR'),
        loadComponent: () => import('./pages/nursing/nursing').then(m => m.Nursing) },

      // scheduling & messaging
      { path: 'appointments', title: 'Appointments', canActivate: [authGuard], data: roles('RECEPTIONIST', 'DOCTOR', 'NURSE', 'ADMIN'),
        loadComponent: () => import('./pages/scheduling/appointments').then(m => m.Appointments) },
      { path: 'messages', title: 'Messages', canActivate: [authGuard], data: roles('DOCTOR'),
        loadComponent: () => import('./pages/messages/messages').then(m => m.Messages) },

      // admin
      { path: 'admin/users', title: 'Users', canActivate: [authGuard], data: roles('ADMIN'),
        loadComponent: () => import('./pages/admin/users').then(m => m.Users) },
      { path: 'admin/departments', title: 'Departments', canActivate: [authGuard], data: roles('ADMIN'),
        loadComponent: () => import('./pages/admin/departments').then(m => m.Departments) },
      { path: 'admin/master-data', title: 'Master data', canActivate: [authGuard], data: roles('ADMIN'),
        loadComponent: () => import('./pages/admin/master-data').then(m => m.MasterData) },
      { path: 'admin/audit', title: 'Audit log', canActivate: [authGuard], data: roles('ADMIN'),
        loadComponent: () => import('./pages/admin/audit').then(m => m.Audit) },

      // patient portal
      { path: 'portal/health', title: 'My health record', canActivate: [authGuard], data: roles('PATIENT'),
        loadComponent: () => import('./pages/portal/portal-health').then(m => m.PortalHealth) },
      { path: 'portal/appointments', title: 'My appointments', canActivate: [authGuard], data: roles('PATIENT'),
        loadComponent: () => import('./pages/portal/portal-appointments').then(m => m.PortalAppointments) },
      { path: 'portal/messages', title: 'Messages', canActivate: [authGuard], data: roles('PATIENT'),
        loadComponent: () => import('./pages/messages/messages').then(m => m.Messages) },
    ],
  },
  { path: '**', redirectTo: '' },
];

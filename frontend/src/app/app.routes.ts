import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { Login } from './auth/login/login';
import { Register } from './auth/register/register';
import { AssetList } from './asset-management/asset-list/asset-list';
import { AssetDetail } from './asset-management/asset-detail/asset-detail';
import { AssetForm } from './asset-management/asset-form/asset-form';
import { DueMaintenanceList } from './due-maintenance/due-maintenance-list/due-maintenance-list';
import { WorkshopList } from './scheduling/workshop-list/workshop-list';
import { SlotCalendar } from './booking/slot-calendar/slot-calendar';
import { PartList } from './inventory/part-list/part-list';
import { WorkOrderList } from './work-order/work-order-list/work-order-list';
import { BreakdownList } from './sla-breakdown/breakdown-list/breakdown-list';
import { DashboardHome } from './dashboard/dashboard-home/dashboard-home';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'dashboard', component: DashboardHome, canActivate: [authGuard] },
  { path: 'assets', component: AssetList, canActivate: [authGuard] },
  { path: 'assets/new', component: AssetForm, canActivate: [authGuard] },
  { path: 'assets/:id', component: AssetDetail, canActivate: [authGuard] },
  { path: 'due-maintenance', component: DueMaintenanceList, canActivate: [authGuard] },
  { path: 'workshops', component: WorkshopList, canActivate: [authGuard] },
  { path: 'bookings', component: SlotCalendar, canActivate: [authGuard] },
  { path: 'inventory', component: PartList, canActivate: [authGuard] },
  { path: 'work-orders', component: WorkOrderList, canActivate: [authGuard] },
  { path: 'breakdowns', component: BreakdownList, canActivate: [authGuard] },
  { path: '**', redirectTo: 'dashboard' },
];

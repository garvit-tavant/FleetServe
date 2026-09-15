import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { Layout } from './shared/layout/layout';
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
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardHome },
      { path: 'assets', component: AssetList },
      { path: 'assets/new', component: AssetForm },
      { path: 'assets/:id', component: AssetDetail },
      { path: 'due-maintenance', component: DueMaintenanceList },
      { path: 'workshops', component: WorkshopList },
      { path: 'bookings', component: SlotCalendar },
      { path: 'inventory', component: PartList },
      { path: 'work-orders', component: WorkOrderList },
      { path: 'breakdowns', component: BreakdownList },
    ],
  },
  { path: '**', redirectTo: '' },
];

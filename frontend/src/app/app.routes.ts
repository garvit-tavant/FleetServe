import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { Layout } from './shared/layout/layout';
import { Login } from './auth/login/login';
import { Register } from './auth/register/register';
import { AssetList } from './asset-management/asset-list/asset-list';
import { AssetDetail } from './asset-management/asset-detail/asset-detail';
import { AssetForm } from './asset-management/asset-form/asset-form';
import { AssetClassList } from './asset-management/asset-class-list/asset-class-list';
import { AssetClassDetail } from './asset-management/asset-class-detail/asset-class-detail';
import { AssetClassForm } from './asset-management/asset-class-form/asset-class-form';
import { WorkshopList } from './workshop-management/workshop-list/workshop-list';
import { WorkshopForm } from './workshop-management/workshop-form/workshop-form';
import { WorkshopDetail } from './workshop-management/workshop-detail/workshop-detail';
import { BayForm } from './workshop-management/bay-form/bay-form';
import { TechnicianList } from './workshop-management/technician-list/technician-list';
import { TechnicianForm } from './workshop-management/technician-form/technician-form';
import { DueMaintenanceList } from './due-maintenance/due-maintenance-list/due-maintenance-list';
import { SlotCalendar } from './booking/slot-calendar/slot-calendar';
import { BookingList } from './booking/booking-list/booking-list';
import { BookingDetail } from './booking/booking-detail/booking-detail';
import { PartList } from './inventory/part-list/part-list';
import { WorkOrderList } from './work-order/work-order-list/work-order-list';
import { WorkOrderDetail } from './work-order/work-order-detail/work-order-detail';
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
      { path: 'asset-classes', component: AssetClassList },
      { path: 'asset-classes/new', component: AssetClassForm },
      { path: 'asset-classes/:id', component: AssetClassDetail },
      { path: 'workshops', component: WorkshopList },
      { path: 'workshops/new', component: WorkshopForm },
      { path: 'workshops/:id', component: WorkshopDetail },
      { path: 'workshops/:workshopId/bays/new', component: BayForm },
      { path: 'technicians', component: TechnicianList },
      { path: 'technicians/new', component: TechnicianForm },
      { path: 'due-maintenance', component: DueMaintenanceList },
      { path: 'bookings', component: BookingList },
      { path: 'bookings/calendar', component: SlotCalendar },
      { path: 'bookings/:id', component: BookingDetail },
      { path: 'inventory', component: PartList },
      { path: 'work-orders', component: WorkOrderList },
      { path: 'work-orders/:id', component: WorkOrderDetail },
      { path: 'breakdowns', component: BreakdownList },
    ],
  },
  { path: '**', redirectTo: '' },
];

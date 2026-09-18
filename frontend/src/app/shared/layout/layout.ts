import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { Auth } from '../../core/services/auth';

interface NavItem {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private auth = inject(Auth);
  private breakpointObserver = inject(BreakpointObserver);

  currentUser = computed(() => this.auth.getCurrentUser());

  navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard' },
    { label: 'Assets', path: '/assets', icon: 'directions_car' },
    { label: 'Asset Classes', path: '/asset-classes', icon: 'category' },
    { label: 'Due Maintenance', path: '/due-maintenance', icon: 'build' },
    { label: 'Workshops', path: '/workshops', icon: 'engineering' },
    { label: 'Technicians', path: '/technicians', icon: 'person' },
    { label: 'Bookings', path: '/bookings', icon: 'event' },
    { label: 'Inventory', path: '/inventory', icon: 'inventory_2' },
    { label: 'Work Orders', path: '/work-orders', icon: 'assignment' },
    { label: 'Breakdowns', path: '/breakdowns', icon: 'warning' },
    { label: 'SLA Reports', path: '/sla-reports', icon: 'assessment' },
  ];

  isHandset = toSignal(
    this.breakpointObserver.observe(Breakpoints.Handset).pipe(map((result) => result.matches)),
    { initialValue: false }
  );

  sidenavOpened = signal(true);

  toggleSidenav(): void {
    this.sidenavOpened.update((v) => !v);
  }

  logout(): void {
    this.auth.logout();
  }
}

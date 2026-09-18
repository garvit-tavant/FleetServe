import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap, map } from 'rxjs/operators';

import { AssetService } from '../../asset-management/services/asset';
import { WorkOrderService } from '../../work-order/services/work-order';
import { DueMaintenanceService } from '../../due-maintenance/services/due-maintenance';
import { SlaReportService, SlaComplianceReport } from '../../sla-breakdown/services/sla-report';
import { PartStockService } from '../../inventory/services/part-stock';
import { WorkshopService } from '../../workshop-management/services/workshop';

interface StatusCount {
  label: string;
  count: number;
}

@Component({
  selector: 'app-dashboard-home',
  imports: [CommonModule, RouterLink, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHome implements OnInit {
  private assetService = inject(AssetService);
  private workOrderService = inject(WorkOrderService);
  private dueMaintenanceService = inject(DueMaintenanceService);
  private slaReportService = inject(SlaReportService);
  private partStockService = inject(PartStockService);
  private workshopService = inject(WorkshopService);

  loading = signal(true);
  errorMessage = signal<string | null>(null);

  assetStatusCounts = signal<StatusCount[]>([]);
  totalAssets = signal(0);

  workOrderStatusCounts = signal<StatusCount[]>([]);
  totalWorkOrders = signal(0);

  dueSoonCount = signal(0);
  overdueCount = signal(0);

  slaCompliance = signal<SlaComplianceReport[]>([]);
  overallCompliancePercent = signal<number | null>(null);

  reorderAlertCount = signal(0);

  ngOnInit(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      assets: this.assetService.getAll().pipe(catchError(() => of([]))),
      workOrders: this.workOrderService.getAll().pipe(catchError(() => of([]))),
      dueMaintenance: this.dueMaintenanceService
        .getDueMaintenanceAssets()
        .pipe(catchError(() => of([]))),
      slaCompliance: this.slaReportService.getSlaCompliance().pipe(catchError(() => of([]))),
      // No "all workshops" reorder-alerts endpoint exists; the API requires a
      // workshopId per call, so we fetch the workshop list first and then
      // fan out one reorder-alerts call per workshop. Fine for the current
      // seed-data scale; would need a dedicated aggregate endpoint at scale.
      reorderAlerts: this.workshopService.getAll().pipe(
        catchError(() => of([])),
        switchMap((workshops) => {
          if (!workshops.length) return of([] as unknown[]);
          return forkJoin(
            workshops.map((w) =>
              this.partStockService.getReorderAlerts(w.id).pipe(catchError(() => of([])))
            )
          ).pipe(map((lists) => lists.flat()));
        })
      ),
    }).subscribe({
      next: ({ assets, workOrders, dueMaintenance, slaCompliance, reorderAlerts }) => {
        this.totalAssets.set(assets.length);
        this.assetStatusCounts.set(this.countBy(assets, (a) => a.status));

        this.totalWorkOrders.set(workOrders.length);
        this.workOrderStatusCounts.set(this.countBy(workOrders, (w) => w.status));

        this.dueSoonCount.set(dueMaintenance.filter((d) => d.dueStatus === 'DUE_SOON').length);
        this.overdueCount.set(dueMaintenance.filter((d) => d.dueStatus === 'OVERDUE').length);

        this.slaCompliance.set(slaCompliance);
        this.overallCompliancePercent.set(this.averageCompliance(slaCompliance));

        this.reorderAlertCount.set(reorderAlerts.length);

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Failed to load dashboard data.');
      },
    });
  }

  private countBy<T>(items: T[], keyFn: (item: T) => string): StatusCount[] {
    const map = new Map<string, number>();
    for (const item of items) {
      const key = keyFn(item);
      map.set(key, (map.get(key) ?? 0) + 1);
    }
    return Array.from(map.entries()).map(([label, count]) => ({ label, count }));
  }

  private averageCompliance(reports: SlaComplianceReport[]): number | null {
    if (!reports.length) return null;
    const totalEvaluated = reports.reduce((sum, r) => sum + r.evaluatedCases, 0);
    if (totalEvaluated === 0) return null;
    const totalCompliant = reports.reduce((sum, r) => sum + r.compliantCases, 0);
    return (totalCompliant / totalEvaluated) * 100;
  }
}

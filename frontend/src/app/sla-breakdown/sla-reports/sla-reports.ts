import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  SlaReportService,
  MeanTimeToRepairReport,
  SlaComplianceReport,
} from '../services/sla-report';

@Component({
  selector: 'app-sla-reports',
  imports: [CommonModule, MatTableModule, MatProgressSpinnerModule],
  templateUrl: './sla-reports.html',
  styleUrl: './sla-reports.scss',
})
export class SlaReports implements OnInit {
  private slaReportService = inject(SlaReportService);

  mttr = signal<MeanTimeToRepairReport[]>([]);
  compliance = signal<SlaComplianceReport[]>([]);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  mttrColumns = ['assetClassCode', 'meanTimeToRepairMinutes', 'sampleSize'];
  complianceColumns = ['priority', 'evaluatedCases', 'compliantCases', 'compliancePercent'];

  ngOnInit(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.slaReportService.getMttr().subscribe({
      next: (data) => this.mttr.set(data),
      error: (err) => this.errorMessage.set(err.error?.message || 'Failed to load MTTR report.'),
    });

    this.slaReportService.getSlaCompliance().subscribe({
      next: (data) => {
        this.compliance.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load SLA compliance report.');
      },
    });
  }
}

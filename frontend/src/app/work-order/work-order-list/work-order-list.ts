import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { WorkOrderService, WorkOrderResponse, WorkOrderStatus } from '../services/work-order';

@Component({
  selector: 'app-work-order-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
  ],
  templateUrl: './work-order-list.html',
  styleUrl: './work-order-list.scss',
})
export class WorkOrderList implements OnInit {
  private workOrderService = inject(WorkOrderService);

  workOrders = signal<WorkOrderResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  statusFilter = signal<WorkOrderStatus | 'ALL'>('ALL');

  statuses: (WorkOrderStatus | 'ALL')[] = [
    'ALL',
    'SCHEDULED',
    'IN_PROGRESS',
    'AWAITING_PARTS',
    'COMPLETED',
    'CANCELLED',
  ];

  displayedColumns = [
    'workOrderNumber',
    'status',
    'startedAt',
    'completedAt',
    'totalCost',
    'actions',
  ];

  filteredWorkOrders = () => {
    const filter = this.statusFilter();
    const all = this.workOrders();
    return filter === 'ALL' ? all : all.filter((w) => w.status === filter);
  };

  ngOnInit(): void {
    this.loadWorkOrders();
  }

  loadWorkOrders(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.workOrderService.getAll().subscribe({
      next: (data) => {
        this.workOrders.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load work orders.');
      },
    });
  }
}

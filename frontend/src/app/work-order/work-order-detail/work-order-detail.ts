import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  WorkOrderService,
  WorkOrderDetailsResponse,
} from '../services/work-order';

@Component({
  selector: 'app-work-order-detail',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './work-order-detail.html',
  styleUrl: './work-order-detail.scss',
})
export class WorkOrderDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private workOrderService = inject(WorkOrderService);
  private fb = inject(FormBuilder);

  workOrder = signal<WorkOrderDetailsResponse | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  actionPending = signal(false);
  actionError = signal<string | null>(null);

  labourColumns = ['technicianName', 'hours', 'rateApplied', 'labourCost'];
  partColumns = ['partCode', 'quantity', 'unitCost', 'lineCost'];

  completeForm = this.fb.group({
    odometerAtService: [null as number | null, [Validators.required, Validators.min(0)]],
    hoursWorked: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  awaitPartsForm = this.fb.group({
    partId: [null as number | null, Validators.required],
    quantityRequired: [null as number | null, [Validators.required, Validators.min(0.01)]],
    reason: ['', Validators.required],
  });

  issuePartForm = this.fb.group({
    partId: [null as number | null, Validators.required],
    quantity: [null as number | null, [Validators.required, Validators.min(0.001)]],
  });

  private get workOrderId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }

  ngOnInit(): void {
    this.loadWorkOrder();
  }

  loadWorkOrder(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.workOrderService.getById(this.workOrderId).subscribe({
      next: (data) => {
        this.workOrder.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load work order.');
      },
    });
  }

  start(): void {
    this.runAction(this.workOrderService.start(this.workOrderId));
  }

  resume(): void {
    this.runAction(this.workOrderService.resume(this.workOrderId));
  }

  complete(): void {
    if (this.completeForm.invalid) {
      this.completeForm.markAllAsTouched();
      return;
    }

    const idempotencyKey =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `${this.workOrderId}-${Date.now()}`;

    this.runAction(
      this.workOrderService.complete(
        this.workOrderId,
        {
          odometerAtService: this.completeForm.value.odometerAtService!,
          hoursWorked: this.completeForm.value.hoursWorked!,
        },
        idempotencyKey
      )
    );
  }

  awaitParts(): void {
    if (this.awaitPartsForm.invalid) {
      this.awaitPartsForm.markAllAsTouched();
      return;
    }

    this.runAction(
      this.workOrderService.awaitParts(this.workOrderId, {
        partId: this.awaitPartsForm.value.partId!,
        quantityRequired: this.awaitPartsForm.value.quantityRequired!,
        reason: this.awaitPartsForm.value.reason!,
      })
    );
  }

  issuePart(): void {
    if (this.issuePartForm.invalid) {
      this.issuePartForm.markAllAsTouched();
      return;
    }

    this.runAction(
      this.workOrderService.issuePart(this.workOrderId, {
        partId: this.issuePartForm.value.partId!,
        quantity: this.issuePartForm.value.quantity!,
      })
    );
  }

  private runAction(obs: import('rxjs').Observable<unknown>): void {
    this.actionPending.set(true);
    this.actionError.set(null);

    obs.subscribe({
      next: () => {
        this.actionPending.set(false);
        this.loadWorkOrder();
      },
      error: (err) => {
        this.actionPending.set(false);
        this.actionError.set(err.error?.message || 'Action failed.');
      },
    });
  }
}

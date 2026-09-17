import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PartService, PartResponse } from '../services/part';
import { PartStockService, PartStockResponse } from '../services/part-stock';
import {
  InventoryMovementService,
  InventoryMovementResponse,
} from '../services/inventory-movement';
import { WorkshopService, WorkshopResponse } from '../../workshop-management/services/workshop';

@Component({
  selector: 'app-part-list',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './part-list.html',
  styleUrl: './part-list.scss',
})
export class PartList implements OnInit {
  private fb = inject(FormBuilder);
  private partService = inject(PartService);
  private partStockService = inject(PartStockService);
  private movementService = inject(InventoryMovementService);
  private workshopService = inject(WorkshopService);

  // Parts catalog
  parts = signal<PartResponse[]>([]);
  partsLoading = signal(false);
  partsError = signal<string | null>(null);
  partColumns = ['partNumber', 'description', 'unitOfMeasure', 'standardCost', 'active'];

  partForm = this.fb.group({
    partNumber: ['', Validators.required],
    description: ['', Validators.required],
    unitOfMeasure: ['', Validators.required],
    standardCost: [null as number | null, [Validators.required, Validators.min(0)]],
  });
  partSubmitting = signal(false);
  partFormError = signal<string | null>(null);

  // Stock dashboard
  workshops = signal<WorkshopResponse[]>([]);
  stockWorkshopId = signal<number | null>(null);
  stock = signal<PartStockResponse[]>([]);
  stockLoading = signal(false);
  stockError = signal<string | null>(null);
  stockColumns = ['partNumber', 'partDescription', 'onHand', 'reorderLevel', 'reorderRequired'];

  // Ledger
  ledgerWorkshopId = signal<number | null>(null);
  movements = signal<InventoryMovementResponse[]>([]);
  ledgerLoading = signal(false);
  ledgerError = signal<string | null>(null);
  ledgerColumns = [
    'occurredAt',
    'partNumber',
    'movementType',
    'signedQuantity',
    'unitCost',
    'reason',
    'recordedBy',
  ];

  movementForm = this.fb.group({
    action: ['RECEIVE', Validators.required],
    partId: [null as number | null, Validators.required],
    workshopId: [null as number | null, Validators.required],
    quantity: [null as number | null, [Validators.required, Validators.min(0.01)]],
    unitCost: [null as number | null, [Validators.required, Validators.min(0)]],
    reason: [''],
  });
  movementSubmitting = signal(false);
  movementError = signal<string | null>(null);

  ngOnInit(): void {
    this.loadParts();
    this.workshopService.getAll().subscribe({
      next: (data) => this.workshops.set(data),
      error: () => undefined,
    });
  }

  loadParts(): void {
    this.partsLoading.set(true);
    this.partsError.set(null);
    this.partService.getAll(0, 200).subscribe({
      next: (page) => {
        this.parts.set(page.content);
        this.partsLoading.set(false);
      },
      error: (err) => {
        this.partsLoading.set(false);
        this.partsError.set(err.error?.message || 'Failed to load parts.');
      },
    });
  }

  createPart(): void {
    if (this.partForm.invalid) {
      this.partForm.markAllAsTouched();
      return;
    }
    const value = this.partForm.getRawValue();
    this.partSubmitting.set(true);
    this.partFormError.set(null);
    this.partService
      .create({
        partNumber: value.partNumber!,
        description: value.description!,
        unitOfMeasure: value.unitOfMeasure!,
        standardCost: value.standardCost!,
      })
      .subscribe({
        next: () => {
          this.partSubmitting.set(false);
          this.partForm.reset();
          this.loadParts();
        },
        error: (err) => {
          this.partSubmitting.set(false);
          this.partFormError.set(err.error?.message || 'Failed to create part.');
        },
      });
  }

  onStockWorkshopChange(workshopId: number): void {
    this.stockWorkshopId.set(workshopId);
    this.stockLoading.set(true);
    this.stockError.set(null);
    this.partStockService.getForWorkshop(workshopId).subscribe({
      next: (data) => {
        this.stock.set(data);
        this.stockLoading.set(false);
      },
      error: (err) => {
        this.stockLoading.set(false);
        this.stockError.set(err.error?.message || 'Failed to load stock.');
      },
    });
  }

  onLedgerWorkshopChange(workshopId: number): void {
    this.ledgerWorkshopId.set(workshopId);
    this.loadLedger();
  }

  loadLedger(): void {
    const workshopId = this.ledgerWorkshopId();
    if (!workshopId) return;
    this.ledgerLoading.set(true);
    this.ledgerError.set(null);
    this.movementService.getMovementsForWorkshop(workshopId, 0, 100).subscribe({
      next: (page) => {
        this.movements.set(page.content);
        this.ledgerLoading.set(false);
      },
      error: (err) => {
        this.ledgerLoading.set(false);
        this.ledgerError.set(err.error?.message || 'Failed to load ledger.');
      },
    });
  }

  submitMovement(): void {
    if (this.movementForm.invalid) {
      this.movementForm.markAllAsTouched();
      return;
    }
    const value = this.movementForm.getRawValue();
    const payload = {
      partId: value.partId!,
      workshopId: value.workshopId!,
      quantity: value.quantity!,
      unitCost: value.unitCost!,
      reason: value.reason || undefined,
    };

    this.movementSubmitting.set(true);
    this.movementError.set(null);

    let request$;
    switch (value.action) {
      case 'ISSUE':
        request$ = this.movementService.issuePart(payload);
        break;
      case 'RETURN':
        request$ = this.movementService.returnPart(payload);
        break;
      case 'ADJUST':
        request$ = this.movementService.adjustStock({
          partId: payload.partId,
          workshopId: payload.workshopId,
          adjustmentQuantity: payload.quantity,
          reason: payload.reason || '',
        });
        break;
      default:
        request$ = this.movementService.receiveStock(payload);
    }

    request$.subscribe({
      next: () => {
        this.movementSubmitting.set(false);
        this.movementForm.reset({ action: 'RECEIVE', reason: '' });
        if (this.ledgerWorkshopId() === value.workshopId) {
          this.loadLedger();
        }
        if (this.stockWorkshopId() === value.workshopId) {
          this.onStockWorkshopChange(value.workshopId!);
        }
      },
      error: (err) => {
        this.movementSubmitting.set(false);
        this.movementError.set(err.error?.message || 'Failed to record movement.');
      },
    });
  }
}

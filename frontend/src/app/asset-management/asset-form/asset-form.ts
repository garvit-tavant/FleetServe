import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from '../services/asset';
import { AssetClassService, AssetClassResponse } from '../services/asset-class';
import { DepotService, DepotResponse } from '../services/depot';

@Component({
  selector: 'app-asset-form',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-form.html',
  styleUrl: './asset-form.scss',
})
export class AssetForm implements OnInit {
  private fb = inject(FormBuilder);
  private assetService = inject(AssetService);
  private assetClassService = inject(AssetClassService);
  private depotService = inject(DepotService);
  private router = inject(Router);

  assetClasses = signal<AssetClassResponse[]>([]);
  depots = signal<DepotResponse[]>([]);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.group({
    vin: ['', [Validators.required, Validators.maxLength(32)]],
    assetClassId: [null as number | null, Validators.required],
    homeDepotId: [null as number | null, Validators.required],
    acquisitionDate: [null as Date | null, Validators.required],
    acquisitionOdometerKm: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.assetClassService.getAll().subscribe({
      next: (data) => this.assetClasses.set(data),
      error: () => this.errorMessage.set('Failed to load asset classes.'),
    });

    this.depotService.getAll().subscribe({
      next: (data) => this.depots.set(data),
      error: () => this.errorMessage.set('Failed to load depots.'),
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const acquisitionDate = value.acquisitionDate as Date;

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.assetService
      .register({
        vin: value.vin!,
        assetClassId: value.assetClassId!,
        homeDepotId: value.homeDepotId!,
        acquisitionDate: acquisitionDate.toISOString().split('T')[0],
        acquisitionOdometerKm: value.acquisitionOdometerKm!,
      })
      .subscribe({
        next: (asset) => {
          this.submitting.set(false);
          this.router.navigate(['/assets', asset.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to register asset.');
        },
      });
  }
}

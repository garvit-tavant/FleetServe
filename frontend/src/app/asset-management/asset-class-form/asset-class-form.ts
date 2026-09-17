import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetClassService } from '../services/asset-class';

@Component({
  selector: 'app-asset-class-form',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-class-form.html',
  styleUrl: './asset-class-form.scss',
})
export class AssetClassForm {
  private fb = inject(FormBuilder);
  private assetClassService = inject(AssetClassService);
  private router = inject(Router);

  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(32)]],
    description: ['', [Validators.required, Validators.maxLength(255)]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.assetClassService
      .create({ code: value.code!, description: value.description! })
      .subscribe({
        next: (assetClass) => {
          this.submitting.set(false);
          this.router.navigate(['/asset-classes', assetClass.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create asset class.');
        },
      });
  }
}

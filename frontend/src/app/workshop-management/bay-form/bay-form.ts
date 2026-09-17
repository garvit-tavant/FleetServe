import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BayService } from '../services/bay';

@Component({
  selector: 'app-bay-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './bay-form.html',
  styleUrl: './bay-form.scss',
})
export class BayForm {
  private fb = inject(FormBuilder);
  private bayService = inject(BayService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.group({
    bayCode: ['', [Validators.required, Validators.maxLength(30)]],
  });

  private get workshopId(): number {
    return Number(this.route.snapshot.paramMap.get('workshopId'));
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.bayService.create(this.workshopId, { bayCode: value.bayCode! }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/workshops', this.workshopId]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to create bay.');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/workshops', this.workshopId]);
  }
}

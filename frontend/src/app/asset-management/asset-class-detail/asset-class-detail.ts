import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetClassService, AssetClassResponse } from '../services/asset-class';

@Component({
  selector: 'app-asset-class-detail',
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './asset-class-detail.html',
  styleUrl: './asset-class-detail.scss',
})
export class AssetClassDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private assetClassService = inject(AssetClassService);

  assetClass = signal<AssetClassResponse | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading.set(true);

    this.assetClassService.getById(id).subscribe({
      next: (data) => {
        this.assetClass.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load asset class.');
      },
    });
  }
}

import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService, AssetSummaryResponse, AssetStatus } from '../services/asset';

@Component({
  selector: 'app-asset-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-list.html',
  styleUrl: './asset-list.scss',
})
export class AssetList implements OnInit {
  private assetService = inject(AssetService);
  private router = inject(Router);

  assets = signal<AssetSummaryResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  displayedColumns = ['vin', 'assetClassCode', 'status'];

  ngOnInit(): void {
    this.loadAssets();
  }

  loadAssets(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.assetService.getAll().subscribe({
      next: (data) => {
        this.assets.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load assets.');
      },
    });
  }

  onRowClick(asset: AssetSummaryResponse): void {
    this.router.navigate(['/assets', asset.id]);
  }

  chipColor(status: AssetStatus): string {
    switch (status) {
      case 'ACTIVE':
      case 'IN_SERVICE':
        return 'primary';
      case 'RETIRED':
        return 'warn';
      default:
        return '';
    }
  }
}

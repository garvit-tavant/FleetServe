import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetClassService, AssetClassResponse } from '../services/asset-class';

@Component({
  selector: 'app-asset-class-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-class-list.html',
  styleUrl: './asset-class-list.scss',
})
export class AssetClassList implements OnInit {
  private assetClassService = inject(AssetClassService);
  private router = inject(Router);

  assetClasses = signal<AssetClassResponse[]>([]);
  loading = signal(false);

  displayedColumns = ['code', 'description'];

  ngOnInit(): void {
    this.loadAssetClasses();
  }

  loadAssetClasses(): void {
    this.loading.set(true);

    this.assetClassService.getAll().subscribe({
      next: (data) => {
        this.assetClasses.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onRowClick(assetClass: AssetClassResponse): void {
    this.router.navigate(['/asset-classes', assetClass.id]);
  }
}

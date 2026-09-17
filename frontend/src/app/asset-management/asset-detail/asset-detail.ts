import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { AssetService, AssetResponse, AssetStatus } from '../services/asset';
import { ReasonDialog, ReasonDialogData } from './reason-dialog';

@Component({
  selector: 'app-asset-detail',
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-detail.html',
  styleUrl: './asset-detail.scss',
})
export class AssetDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private assetService = inject(AssetService);
  private dialog = inject(MatDialog);

  asset = signal<AssetResponse | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadAsset();
  }

  private get assetId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }

  loadAsset(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.assetService.getById(this.assetId).subscribe({
      next: (data) => {
        this.asset.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load asset.');
      },
    });
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

  openRetireDialog(): void {
    this.openReasonDialog(
      { title: 'Retire Asset', actionLabel: 'Retire' },
      (reason) => this.assetService.retire(this.assetId, { reason })
    );
  }

  openReinstateDialog(): void {
    this.openReasonDialog(
      { title: 'Reinstate Asset', actionLabel: 'Reinstate' },
      (reason) => this.assetService.reinstate(this.assetId, { reason })
    );
  }

  private openReasonDialog(
    data: ReasonDialogData,
    action: (reason: string) => import('rxjs').Observable<void>
  ): void {
    const dialogRef = this.dialog.open(ReasonDialog, { data, width: '400px' });

    dialogRef.afterClosed().subscribe((reason: string | undefined) => {
      if (!reason) {
        return;
      }

      action(reason).subscribe({
        next: () => this.loadAsset(),
        error: (err) => {
          this.errorMessage.set(err.error?.message || 'Action failed.');
        },
      });
    });
  }
}

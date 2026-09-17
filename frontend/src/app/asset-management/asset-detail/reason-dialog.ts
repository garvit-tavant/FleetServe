import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface ReasonDialogData {
  title: string;
  actionLabel: string;
}

@Component({
  selector: 'app-reason-dialog',
  imports: [FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './reason-dialog.html',
})
export class ReasonDialog {
  private dialogRef = inject(MatDialogRef<ReasonDialog>);
  data: ReasonDialogData = inject(MAT_DIALOG_DATA);

  reason = '';

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    if (!this.reason || !this.reason.trim()) {
      return;
    }
    this.dialogRef.close(this.reason.trim());
  }
}

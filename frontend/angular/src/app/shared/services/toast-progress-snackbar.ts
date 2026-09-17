import { ChangeDetectionStrategy, Component, Signal, effect, inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA, MatSnackBarLabel, MatSnackBarRef } from '@angular/material/snack-bar';

export interface ToastProgressData {
  message: string;
  progress: Signal<number | null>;
}

@Component({
  selector: 'app-toast-progress-snackbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatSnackBarLabel],
  template: `<div matSnackBarLabel>{{ data.message }} {{ data.progress() ?? 0 }}%</div>`,
})
export class ToastProgressSnackbar {
  readonly data = inject<ToastProgressData>(MAT_SNACK_BAR_DATA);
  private readonly snackBarRef = inject(MatSnackBarRef<ToastProgressSnackbar>);

  constructor() {
    effect(() => {
      if (this.data.progress() === null) {
        this.snackBarRef.dismiss();
      }
    });
  }
}

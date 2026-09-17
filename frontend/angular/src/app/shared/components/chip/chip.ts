import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

export type ChipVariant = 'tonal' | 'filled' | 'muted';

@Component({
  selector: 'app-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon, MatTooltip],
  template: `
    <ng-content />
    @if (clearable()) {
      <button
        class="chip__clear"
        type="button"
        [attr.aria-label]="clearTooltip()"
        [attr.data-testid]="testId() ? testId() + '-clear' : null"
        [matTooltip]="clearTooltip()"
        [matTooltipDisabled]="!clearTooltip()"
        (click)="clear.emit()">
        <mat-icon>close</mat-icon>
      </button>
    }
  `,
  styleUrl: './chip.scss',
  host: {
    '[class.chip--tonal]': "variant() === 'tonal'",
    '[class.chip--filled]': "variant() === 'filled'",
    '[class.chip--muted]': "variant() === 'muted'",
    '[class.chip--dense]': 'dense()',
    '[class.chip--clearable]': 'clearable()',
    '[attr.data-testid]': 'testId()',
  },
})
export class Chip {
  readonly variant = input<ChipVariant>('tonal');

  /** Tighter padding and no wrapping, for a chip inside a dense table cell. */
  readonly dense = input<boolean>(false);

  /**
   * Adds a trailing button that drops the value, for a chip that stands for a picked reference.
   * Deliberately part of the chip rather than a button beside it: it belongs to this one value,
   * and it has to stay small enough to sit inside a table cell or a form field.
   */
  readonly clearable = input<boolean>(false);

  /** Shown on the clear button, which is icon-only and needs it as its accessible name too. */
  readonly clearTooltip = input<string>('');

  readonly testId = input<string | undefined>(undefined);

  readonly clear = output<void>();
}

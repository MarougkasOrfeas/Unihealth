import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatTooltip, TooltipPosition } from '@angular/material/tooltip';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-button',
  standalone: true,
  imports: [MatIcon, MatIconButton, MatButton, MatTooltip, MatProgressSpinner, TranslatePipe],
  templateUrl: './button.html',
  styleUrl: './button.scss',
})
export class Button {
  buttonStyle = input<'filled' | 'outlined' | 'text'>('filled');
  /** `compact` is icon-button only: a 2rem quick-action target for rows and side panels. */
  size = input<'compact' | 'xsmall' | 'small' | 'medium' | 'large'>('medium');
  variant = input<'primary' | 'danger'>('primary');

  icon = input<string | null>(null);
  iconPosition = input<'left' | 'right'>('left');
  /**
   * If set to true, the {@link ariaLabel} MUST have a non-empty descriptive string to
   * meet accessibility standards
   */
  isIconButton = input<boolean>(false);

  /**
   * If {@link disabledReason} is not provided, the button will be completely disabled (not
   * focusable). Otherwise, the button will be focusable but not clickable, and the
   * {@link disabledReason} will be shown in a tooltip on hover.
   */
  disabled = input<boolean>(false);
  /**
   * The reason will be displayed in a tooltip and will be readable by screen readers, so it
   * should be a user-friendly message explaining why the button is disabled.
   */
  disabledReason = input<string | null>(null);

  loading = input<boolean>(false);
  spinnerDiameter = signal<number>(18);

  /**
   * Should be always provided for icon buttons, while for standard buttons only when the label
   * is not explanatory enough.
   *
   * The tooltip string is replaced by the {@link disabledReason} when provided.
   */
  tooltip = input<string>('');
  tooltipPosition = input<TooltipPosition>('above');

  ariaLabel = input<string>('');

  testId = input<string | null>(null);

  readonly clicked = output<MouseEvent>();

  computedAriaLabel = computed(() => {
    if (this.ariaLabel()) {
      return this.ariaLabel();
    }
    if (this.loading()) {
      return 'Loading, please wait';
    }
    return null;
  });

  handleButtonClick(event: MouseEvent): void {
    if (this.disabled() || this.loading()) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    this.clicked.emit(event);
  }
}

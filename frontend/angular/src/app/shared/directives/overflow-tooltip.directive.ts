import { Directive, ElementRef, HostListener, inject, Input } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';

/**
 * Adds a Material tooltip only when the element's text is truncated (overflow ellipsis).
 * Usage: <span appOverflowTooltip="full text here" class="truncating-cell">...</span>
 */
@Directive({
  selector: '[appOverflowTooltip]',
  standalone: true,
  hostDirectives: [MatTooltip],
  host: {
    '[style.text-overflow]': '"ellipsis"',
  },
})
export class OverflowTooltipDirective {
  @Input('appOverflowTooltip') text = '';
  @Input() appOverflowTooltipDisabled = false;

  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly tooltip = inject(MatTooltip);

  constructor() {
    this.tooltip.disabled = true;
  }

  @HostListener('mouseenter')
  onMouseEnter(): void {
    if (this.appOverflowTooltipDisabled || !this.text.trim()) {
      this.tooltip.hide();
      this.tooltip.disabled = true;
      return;
    }

    const el = this.el.nativeElement;
    if (el.scrollWidth > el.clientWidth) {
      this.tooltip.message = this.text;
      this.tooltip.disabled = false;
      this.tooltip.show();
    }
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.tooltip.hide();
    this.tooltip.disabled = true;
  }
}


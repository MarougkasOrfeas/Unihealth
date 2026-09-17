import { Component, ChangeDetectionStrategy, input, inject, output } from '@angular/core';
import { Button } from '../button/button';
import { TranslatePipe } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { MatChip } from '@angular/material/chips';
import { OverflowTooltipDirective } from '../../directives/overflow-tooltip.directive';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-page-header',
  standalone: true,
  templateUrl: './page-header.html',
  styleUrls: ['./page-header.scss'],
  imports: [Button, TranslatePipe, MatChip, OverflowTooltipDirective]
})
export class PageHeader {
  private readonly router = inject(Router);

  title = input.required<string>();
  hasUnsavedChanges = input<boolean>(false);
  subtitle = input<string>();

  /** 'section' steps the heading down for a header rendered inside a page that already has one. */
  headingScale = input<'page' | 'section'>('page');

  backLink = input<string | unknown[] | null>(null);
  readonly backClick = output<MouseEvent>();

  /** Flag to determine if the back navigation should be handled by the parent component */
  manualBackControl = input<boolean>(false);

  testId = input<string>('page-header');

  handleBackClick(event: MouseEvent): void {
    if (this.manualBackControl()) {
      this.backClick.emit(event);
    } else if (this.backLink()) {
      this.router.navigate([this.backLink()]);
    }
  }
}

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Button } from '../button/button';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [TranslateModule, RouterLink, Button],
  templateUrl: './no-data-table.html',
  styleUrls: ['./no-data-table.scss'],
})
export class NoDataTable {
  @Input() titleKey = '';
  @Input() subtitleKey = '';

  @Input() hasButton = false;
  @Input() buttonLabelKey = '';
  @Input() buttonTestId = 'btn-empty-state-create';
  @Input() routerLink?: string | unknown[];
  @Input() disabled = false;

  @Input() testId = 'empty-state';
  @Input() titleTestId = 'empty-state-title';
  @Input() subtitleTestId = 'empty-state-subtitle';

  @Output() create = new EventEmitter<void>();

  onCreate(): void {
    if (this.routerLink) {
      return;
    }
    this.create.emit();
  }
}

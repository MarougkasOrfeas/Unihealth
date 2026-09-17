import {Component, EventEmitter, Input, Output} from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatChip, MatChipSet } from '@angular/material/chips';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import {TranslateModule} from '@ngx-translate/core';

export interface ActiveFilter {
  key: string;
  /** Lexicon key of the column label; translated by the template. */
  label: string;
  /** Raw backend values. `getValueLabel` turns each into what the user sees. */
  values: string[];
}

@Component({
  selector: 'app-active-filters-toolbar',
  standalone: true,
  imports: [MatChipSet, MatChip, MatIcon, MatIconButton, MatTooltip, TranslateModule],
  templateUrl: './active-filters-toolbar.html',
  styleUrl: './active-filters-toolbar.scss',
})
export class ActiveFiltersToolbar {

  private readonly identityLabel = (_key: string, value: string) => value;

  @Input() filters: ActiveFilter[] = [];
  @Input() getValueLabel: (key: string, value: string) => string = this.identityLabel;
  @Output() removeFilter = new EventEmitter<string>();
  @Output() clearAll = new EventEmitter<void>();

  formatValues(key: string, values: string[]): string {
    const labels = values.map((value) => this.getValueLabel(key, value));
    const maxShow = 3;
    if (labels.length <= maxShow) {
      return labels.join(', ');
    }
    return labels.slice(0, maxShow).join(', ') + ` (+${labels.length - maxShow})`;
  }
}


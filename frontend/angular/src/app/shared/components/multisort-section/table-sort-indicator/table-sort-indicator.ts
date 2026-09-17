import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { SortDirection } from '@angular/material/sort';

@Component({
  selector: 'app-table-sort-indicator',
  standalone: true,
  imports: [MatIcon],
  templateUrl: './table-sort-indicator.html',
  styleUrl: './table-sort-indicator.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SortIndicator {
  @Input() sortDir: SortDirection | undefined = '';
  @Input() index?: number;
  @Input() hover = false;

  get isSorted(): boolean {
    return this.sortDir === 'asc' || this.sortDir === 'desc';
  }

  get icon(): string {
    return this.sortDir === 'desc' ? 'arrow_downward' : 'arrow_upward';
  }
}

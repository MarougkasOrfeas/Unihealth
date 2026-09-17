import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import {MatChip, MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { MatTooltip } from '@angular/material/tooltip';

/** Labels are lexicon keys; the template translates them, so a language switch is picked up by the
 *  impure `TranslatePipe` without the producer having to re-emit. */
export interface SortChip {
  key: string;
  label: string;
  dirLabel: string;
}

@Component({
  selector: 'app-multisort-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './multisort-section.html',
  styleUrls: ['./multisort-section.scss'],
  imports: [MatChipSet, MatChip, MatIcon, MatIconButton, TranslatePipe, MatTooltip],
})
export class MultisortSection {
  @Input() activeSorts: SortChip[] = [];
  @Input() label = '';
  /** Lexicon key; translated by the template. */
  @Input() clearTooltip = '';
  @Input() ariaLabel = 'Sort chips';

  @Output() removeSort = new EventEmitter<string>();
  @Output() clearSort = new EventEmitter<void>();
}

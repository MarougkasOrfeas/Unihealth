import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-save-button',
  standalone: true,
  imports: [MatButtonModule, MatTooltipModule, MatIconModule , TranslateModule],
  templateUrl: './save-button.html',
  styleUrls: ['./save-button.scss'],
})
export class SaveButtonComponent {
  @Input() label = 'Speichern';

  @Input() disabled = false;

  @Input() tooltip?: string;

  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';

  @Output() save = new EventEmitter<MouseEvent>();

  onClick(ev: MouseEvent) {
    if (this.disabled) return;
    this.save.emit(ev);
  }
}

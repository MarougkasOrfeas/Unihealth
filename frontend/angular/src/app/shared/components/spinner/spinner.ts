import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import {MatProgressSpinner} from '@angular/material/progress-spinner';

@Component({
  selector: 'app-spinner',
  templateUrl: './spinner.html',
  styleUrls: ['./spinner.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatProgressSpinner
  ]
})
export class SpinnerComponent {
  @Input() text?: string;
  @Input() subtext?: string;
  @Input() fullscreen = true;

  readonly diameter = 50;

  get hasText(): boolean {
    return !!this.text;
  }

  get hasSubtext(): boolean {
    return !!this.subtext;
  }
}

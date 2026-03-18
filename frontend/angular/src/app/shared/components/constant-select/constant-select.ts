import { Component, Input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

export type SelectOption<K extends string = string> = {
  key: K;
  label: string;
};

@Component({
  selector: 'app-constant-select',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatSelectModule, MatTooltipModule, TranslateModule],
  templateUrl: './constant-select.html',
  styleUrls: ['./constant-select.scss'],
})
export class ConstantSelectComponent<K extends string = string> {
  @Input({ required: true }) control!: FormControl<K>;
  @Input({ required: true }) label!: string;
  @Input({ required: true }) options!: ReadonlyArray<SelectOption<K>>;

  @Input() submitted = false;
  @Input() required = false;

  @Input() tooltip?: string;

  private shouldShowErrors(): boolean {
    return this.submitted || this.control.touched || this.control.dirty;
  }

  showError(code: string): boolean {
    return this.shouldShowErrors() && this.control.hasError(code);
  }
}

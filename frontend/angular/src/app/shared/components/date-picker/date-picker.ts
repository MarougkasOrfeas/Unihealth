import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-date-picker',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatTooltipModule,
    TranslatePipe,
  ],
  templateUrl: './date-picker.html',
  styleUrls: ['./date-picker.scss'],
})
export class DatePickerComponent {
  @Input({ required: true }) control!: FormControl<Date | null>;
  @Input({ required: true }) label!: string;

  @Input() required = false;
  @Input() submitted = false;

  @Input() tooltip?: string;

  @Input() placeholder?: string;

  @Input() min?: Date;
  @Input() max?: Date;

  @Output() dateChanged = new EventEmitter<Date | null>();

  private _disabled = false;

  @Input()
  set disabledInput(value: boolean) {
    this._disabled = value;
    if (!this.control) return;

    if (value) {
      this.control.disable({ emitEvent: false });
    } else {
      this.control.enable({ emitEvent: false });
    }
  }

  get disabled(): boolean {
    return this._disabled;
  }

  private shouldShowErrors(): boolean {
    return this.submitted || this.control.touched || this.control.dirty;
  }

  showError(code: string): boolean {
    return this.shouldShowErrors() && this.control.hasError(code);
  }

  showGenericError(): boolean {
    if (!this.shouldShowErrors() || !this.control.errors) return false;
    return !this.control.hasError('required') && !this.control.hasError('matDatepickerParse');
  }


  onDateInput() {
    this.dateChanged.emit(this.control.value);
  }
}

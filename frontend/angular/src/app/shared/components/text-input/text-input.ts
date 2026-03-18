import {CommonModule} from '@angular/common';
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {UNIHEALTH_CONSTANTS} from "../../constants/unihealth.constants";

@Component({
  selector: 'app-text-input',
  templateUrl: './text-input.html',
  styleUrls: ['./text-input.scss'],
  standalone: true,
  imports: [MatFormFieldModule, MatInputModule, MatIconModule, MatTooltipModule, ReactiveFormsModule, CommonModule],
})
export class TextInputComponent {
    @Input({required: true}) control!: FormControl;
    @Input({required: true}) label!: string;

    @Input() required = false;
    @Input() submitted = false;
    @Input() patternErrorKey?: string;

    @Input() tooltip?: string;

    @Input() type: 'text' | 'email' | 'phone' = 'text';
    @Input() autocomplete = 'off';
    @Input() maxlength?: number;
    @Input() disabled = false;
    @Output() blur = new EventEmitter<void>();

    private shouldShowErrors(): boolean {
        return this.submitted || this.control.touched || this.control.dirty;
    }

    showError(errorCode: string): boolean {
        return this.shouldShowErrors() && this.control.hasError(errorCode);
    }

    showGenericError(): boolean {
        if (!this.shouldShowErrors() || !this.control.errors) {
            return false;
        }
        return (!this.control.hasError('required') &&
            !this.control.hasError('minlength') &&
            !this.control.hasError('maxlength') &&
            !this.control.hasError('pattern') &&
            !this.control.hasError('email') &&
            !this.control.hasError('backend')
        );
    }

    genericErrorMessage(): string {
        return 'Μη έγκυρη τιμή.';
    }

    get inputMode(): string {
        switch (this.type) {
            case 'email':
                return 'email';
            case 'phone':
                return 'tel';
            default:
                return 'text';
        }
    }

    get pattern(): string | null {
        switch (this.type) {
            case 'email':
                return UNIHEALTH_CONSTANTS.PATTERNS.EMAIL_PTN;
            case 'phone':
                return UNIHEALTH_CONSTANTS.PATTERNS.PHONE_PTN;
            default:
                return null;
        }
    }
}

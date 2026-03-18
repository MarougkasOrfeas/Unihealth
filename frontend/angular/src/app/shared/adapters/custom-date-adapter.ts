import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/**
 * Custom date adapter for German locale.
 * Handles manual date entry in DD.MM.YYYY format.
 *
 * Supported formats for manual entry:
 * - DD.MM.YYYY (e.g., 15.03.2025)
 * - D.M.YYYY (e.g., 5.3.2025)
 * - DD.MM.YY (e.g., 15.03.25)
 * - D.M.YY (e.g., 5.3.25)
 */
@Injectable()
export class CustomDateAdapter extends NativeDateAdapter {

  /**
   * Override to set German locale
   */
  override getFirstDayOfWeek(): number {
    return 1; // Monday
  }

  /**
   * Parse manually entered dates in German format (DD.MM.YYYY)
   * @param value String input from the user
   * @returns Parsed Date or null if invalid
   */
  override parse(value: string | number): Date | null {
    if (typeof value !== 'string') {
      return super.parse(value);
    }

    // Trim whitespace
    value = value.trim();

    if (!value) {
      return null;
    }

    // Try to parse DD.MM.YYYY or D.M.YYYY format
    const datePattern = /^(\d{1,2})\.(\d{1,2})\.(\d{2,4})$/;
    const match = value.match(datePattern);

    if (!match) {
      // Invalid format - return invalid date to trigger matDatepickerParse error
      return new Date(NaN);
    }

    const day = parseInt(match[1], 10);
    const month = parseInt(match[2], 10);
    let year = parseInt(match[3], 10);

    // Handle 2-digit years: 00-49 → 2000-2049, 50-99 → 1950-1999
    if (year < 100) {
      year += year < 50 ? 2000 : 1900;
    }

    // Validate month range
    if (month < 1 || month > 12) {
      return new Date(NaN);
    }

    // Validate day range for the given month
    const daysInMonth = new Date(year, month, 0).getDate();
    if (day < 1 || day > daysInMonth) {
      return new Date(NaN);
    }

    // Create date at midnight local time to avoid timezone issues
    const date = new Date(year, month - 1, day, 0, 0, 0, 0);

    // Verify the date is valid
    if (isNaN(date.getTime())) {
      return new Date(NaN);
    }

    return date;
  }

  /**
   * Format date for display in DD.MM.YYYY format
   * @param date Date to format
   * @param displayFormat Display format type
   * @returns Formatted date string
   */
  override format(date: Date, displayFormat: Object): string {
    if (!this.isValid(date)) {
      return '';
    }

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();

    return `${day}.${month}.${year}`;
  }
}

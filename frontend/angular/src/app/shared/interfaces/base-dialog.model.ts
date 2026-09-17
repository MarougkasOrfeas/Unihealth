import { TemplateRef } from '@angular/core';

/**
 * The model of the data the BaseDialog component expects.
 */
export type BaseDialogData = {
  title?: string;
  content: string | TemplateRef<unknown>;
  contentData?: Record<string, unknown>;
  cancelText?: string;
  confirmText?: string;
  isDestructive?: boolean;
  hideConfirm?: boolean;
};

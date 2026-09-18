/**
 * Helpers for uploading and listing files.
 *
 * Plain functions rather than pipes, deliberately: `formatBytes` and `fileTypeLabel` are consumed
 * inside a table's `mapToRow`, which is ordinary TypeScript where template syntax does not exist.
 * There is also no pipe anywhere else in this application, and two formatters are a thin reason to
 * introduce the first one.
 */

/** Value for the `accept` attribute. Advisory — the browser does not enforce it. */
export const EXAM_FILE_ACCEPT = 'application/pdf,image/jpeg,image/png,image/tiff';

/** Mirrors the backend's ATTACHMENT upload policy, which is the authority. */
const ACCEPTED_MIME_TYPES: ReadonlySet<string> = new Set([
    'application/pdf', 'image/jpeg', 'image/png', 'image/tiff',
]);

const ACCEPTED_EXTENSIONS: ReadonlySet<string> = new Set([
    'pdf', 'jpg', 'jpeg', 'png', 'tif', 'tiff',
]);

export function extensionOf(name: string): string {
    const dot = name.lastIndexOf('.');
    return dot < 0 ? '' : name.slice(dot + 1).toLowerCase();
}

/**
 * A first pass only — the backend sniffs the actual bytes and has the final say.
 *
 * The extension fallback is not belt and braces: Safari reports an empty `type` for TIFF and some
 * Windows installations report `image/tif`, so trusting the MIME type alone would reject perfectly
 * good files at the door.
 */
export function isAcceptedExamFile(file: File): boolean {
    return ACCEPTED_MIME_TYPES.has(file.type) || ACCEPTED_EXTENSIONS.has(extensionOf(file.name));
}

const BYTES_PER_UNIT = 1024;
const UNITS = ['B', 'KB', 'MB', 'GB'] as const;

/**
 * `0` becomes `0 B`, `1536` becomes `1,5 KB` under a Greek locale.
 *
 * @param locale defaults to the browser's. Pass the UI language if the decimal separator should
 *               follow the chosen language rather than the operating system.
 */
export function formatBytes(bytes: number | null | undefined, locale?: string): string {
    if (bytes == null || !Number.isFinite(bytes) || bytes < 0) {
        return '-';
    }
    if (bytes < BYTES_PER_UNIT) {
        return `${bytes} ${UNITS[0]}`;
    }

    let value = bytes;
    let unit = 0;
    while (value >= BYTES_PER_UNIT && unit < UNITS.length - 1) {
        value /= BYTES_PER_UNIT;
        unit++;
    }

    return `${value.toLocaleString(locale, {maximumFractionDigits: 1})} ${UNITS[unit]}`;
}

/** Material Symbols Outlined names; the font is loaded globally in styles.scss. */
const ICON_BY_MIME: Readonly<Record<string, string>> = {
    'application/pdf': 'picture_as_pdf',
    'image/jpeg': 'image',
    'image/png': 'image',
    'image/tiff': 'image',
};

export function fileIconFor(contentType: string | null | undefined): string {
    return (contentType && ICON_BY_MIME[contentType]) || 'draft';
}

const LABEL_BY_MIME: Readonly<Record<string, string>> = {
    'application/pdf': 'PDF',
    'image/jpeg': 'JPEG',
    'image/png': 'PNG',
    'image/tiff': 'TIFF',
};

/**
 * Short type badge for the list.
 *
 * Not routed through the lexicon on purpose: these are format names, identical in both languages,
 * and four keys that can only ever drift apart would buy nothing.
 */
export function fileTypeLabel(contentType: string | null | undefined): string {
    return (contentType && LABEL_BY_MIME[contentType]) || '-';
}

/**
 * The local calendar date as `yyyy-MM-dd`.
 *
 * Deliberately not `toISOString().slice(0, 10)`. A date picker hands back local midnight, and in
 * Greece (UTC+2/+3) that serialises to the *previous* day in UTC — so every examination date would
 * quietly be stored one day early.
 */
export function toLocalDateString(date: Date): string {
    const pad = (value: number): string => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/**
 * Pulls the file name out of a `Content-Disposition` header.
 *
 * The RFC 5987 form is tried first because that is what a Greek file name arrives in; the plain
 * form is the fallback for an ASCII name.
 */
export function filenameFromContentDisposition(header: string | null): string | null {
    if (!header) {
        return null;
    }

    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header);
    if (encoded) {
        try {
            return decodeURIComponent(encoded[1]);
        } catch {
            // Malformed percent-encoding: fall through to the plain form rather than throwing.
        }
    }

    const plain = /filename="?([^";]+)"?/i.exec(header);
    return plain ? plain[1].trim() : null;
}

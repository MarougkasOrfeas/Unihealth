import {SelectOption} from '../components/constant-select/constant-select';
import {BaseUpdateableEntity} from './baseEntity';

/** Mirrors the backend's MedicalTestCategory. */
export type ExamCategory =
    'BLOOD_TEST' | 'IMAGING' | 'CARDIOLOGY' | 'MICROBIOLOGY' | 'OTHER';

export const EXAM_CATEGORIES: readonly ExamCategory[] =
    ['BLOOD_TEST', 'IMAGING', 'CARDIOLOGY', 'MICROBIOLOGY', 'OTHER'];

/** `label` holds a lexicon key; the select translates it. */
export const EXAM_CATEGORY_OPTIONS: readonly SelectOption<ExamCategory>[] =
    EXAM_CATEGORIES.map((key) => ({key, label: `examFile.category.${key}`}));

export interface ExamFileDTO extends BaseUpdateableEntity {
    name: string;
    description?: string;
    contentType: string;
    /** Bytes. */
    fileSize: number;
    /**
     * When the examination happened, as opposed to `createdOn`, which is when it was uploaded.
     *
     * Already a `Date` by the time a component sees it: `BaseService` rewrites anything shaped like
     * an ISO date on the way in. A bare `2024-05-03` therefore lands as UTC midnight, which still
     * renders as 03/05/2024 anywhere east of Greenwich — correct here, and worth knowing before
     * anyone runs this further west.
     */
    examDate?: Date;
    category?: ExamCategory;
}

/** What `_upload` answers with. */
export interface ExamFileUploadResultDTO {
    fileId: string;
    filename: string;
    contentType: string;
    fileSize: number;
}

export type ExamFileUploadMeta = {
    examDate: Date | null;
    category: ExamCategory;
};

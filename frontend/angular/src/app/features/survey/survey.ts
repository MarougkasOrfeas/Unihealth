import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormControl, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatIcon} from '@angular/material/icon';
import {Router} from '@angular/router';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {map, Observable} from 'rxjs';
import {Button} from '../../shared/components/button/button';
import {PageHeader} from '../../shared/components/page-header/page-header';
import {RadioGroupComponent} from '../../shared/components/radio-group/radio-group';
import {SaveButtonComponent} from '../../shared/components/save-button/save-button';
import {UNIHEALTH_CONSTANTS} from '../../shared/constants/unihealth.constants';
import {CanLeaveWithUnsavedChanges} from '../../shared/interfaces/unsaved-changes';
import {
    SURVEY_AI_OPTIONS,
    SURVEY_COMFORT_OPTIONS,
    SURVEY_EASE_OPTIONS,
    SURVEY_EMAIL_OPTIONS,
    SURVEY_FORM_LENGTH_OPTIONS,
    SURVEY_LANGUAGE_OPTIONS,
    SURVEY_SECTION_OPTIONS,
    SURVEY_SUGGESTION_OPTIONS,
    SURVEY_USEFULNESS_OPTIONS,
    SurveyComfort,
    SurveyDTO,
    SurveyEmailFrequency,
    SurveyFormLength,
    SurveyHelpfulness,
    SurveyLanguageImpact,
    SurveyRating5,
    SurveySection,
    SurveySuggestionMatch,
} from '../../shared/interfaces/survey';
import {MessageService} from '../../shared/services/message.service';
import {SurveyService} from '../../shared/services/survey.service';
import {errorMessageFromHttp} from '../../shared/utils/http-error.util';

/**
 * «Έρευνα UniHealth» — ten questions about what to improve next.
 *
 * <p>One response per student, overwritten on resubmission, which is why the page loads any previous
 * answers and says when they were given rather than presenting a blank form as though nothing had
 * been said before.
 */
@Component({
    selector: 'app-survey',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        Button,
        DatePipe,
        MatIcon,
        PageHeader,
        RadioGroupComponent,
        ReactiveFormsModule,
        SaveButtonComponent,
        TranslatePipe,
    ],
    templateUrl: './survey.html',
    styleUrl: './survey.scss',
})
export class Survey implements OnInit, CanLeaveWithUnsavedChanges {

    private readonly router = inject(Router);
    private readonly surveyService = inject(SurveyService);
    private readonly messages = inject(MessageService);
    private readonly translate = inject(TranslateService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly fb = inject(FormBuilder);

    protected readonly usefulnessOptions = SURVEY_USEFULNESS_OPTIONS;
    protected readonly easeOptions = SURVEY_EASE_OPTIONS;
    protected readonly sectionOptions = SURVEY_SECTION_OPTIONS;
    protected readonly suggestionOptions = SURVEY_SUGGESTION_OPTIONS;
    protected readonly formLengthOptions = SURVEY_FORM_LENGTH_OPTIONS;
    protected readonly aiOptions = SURVEY_AI_OPTIONS;
    protected readonly languageOptions = SURVEY_LANGUAGE_OPTIONS;
    protected readonly comfortOptions = SURVEY_COMFORT_OPTIONS;
    protected readonly emailOptions = SURVEY_EMAIL_OPTIONS;

    protected readonly saving = signal(false);
    protected readonly submitted = signal(false);
    /** When the student last answered, or null if this is their first time. */
    protected readonly answeredOn = signal<Date | null>(null);

    /** The nine required questions. Q10 is excluded — a progress bar that can never reach the end
     *  because of an optional comment box would be a small lie. */
    protected readonly requiredCount = 9;
    protected readonly answeredCount = signal(0);
    protected readonly progressPercent = computed(
        () => Math.round((this.answeredCount() / this.requiredCount) * 100));

    /**
     * Nine required questions and one optional comment.
     *
     * <p>Not `fb.nonNullable`: every control starts as null so that nothing is pre-selected. A radio
     * group defaulted to its first option would quietly collect that answer from anybody who
     * scrolled past, which is worse than no data.
     */
    protected readonly form = this.fb.group({
        overallUsefulness: new FormControl<SurveyRating5 | null>(null, Validators.required),
        easeOfFinding: new FormControl<SurveyRating5 | null>(null, Validators.required),
        mostUsedSection: new FormControl<SurveySection | null>(null, Validators.required),
        suggestionMatch: new FormControl<SurveySuggestionMatch | null>(null, Validators.required),
        formLength: new FormControl<SurveyFormLength | null>(null, Validators.required),
        aiHelpfulness: new FormControl<SurveyHelpfulness | null>(null, Validators.required),
        languageBarrier: new FormControl<SurveyLanguageImpact | null>(null, Validators.required),
        dataComfort: new FormControl<SurveyComfort | null>(null, Validators.required),
        emailFrequency: new FormControl<SurveyEmailFrequency | null>(null, Validators.required),
        improvement: new FormControl<string>('', {nonNullable: true, validators: [Validators.maxLength(1000)]}),
    });

    constructor() {
        // Recounted from the form rather than tracked per control, so loading a previous response
        // and answering by hand both land in the same place.
        this.form.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.answeredCount.set(this.countAnswered()));
    }

    private countAnswered(): number {
        const raw = this.form.getRawValue();
        return [
            raw.overallUsefulness, raw.easeOfFinding, raw.mostUsedSection, raw.suggestionMatch,
            raw.formLength, raw.aiHelpfulness, raw.languageBarrier, raw.dataComfort,
            raw.emailFrequency,
        ].filter((value) => value !== null).length;
    }

    ngOnInit(): void {
        this.surveyService.getMySurvey()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (response) => this.patch(response),
                // A failed load is not worth blocking on: the form still works, and submitting
                // simply creates the response instead of replacing one.
                error: () => this.messages.error(
                    this.translate.instant('survey.load.error')),
            });
    }

    private patch(response: SurveyDTO): void {
        if (!response?.overallUsefulness) {
            return;   // never answered; leave every control untouched
        }

        this.form.patchValue({
            overallUsefulness: response.overallUsefulness ?? null,
            easeOfFinding: response.easeOfFinding ?? null,
            mostUsedSection: response.mostUsedSection ?? null,
            suggestionMatch: response.suggestionMatch ?? null,
            formLength: response.formLength ?? null,
            aiHelpfulness: response.aiHelpfulness ?? null,
            languageBarrier: response.languageBarrier ?? null,
            dataComfort: response.dataComfort ?? null,
            emailFrequency: response.emailFrequency ?? null,
            improvement: response.improvement ?? '',
        });

        this.answeredOn.set(response.modifiedOn ?? null);
    }

    /** Ten answers are worth not losing to a stray click on the sidebar. */
    canDeactivate(): boolean | Observable<boolean> {
        if (!this.form.dirty) {
            return true;
        }
        return this.messages.confirmDiscard().afterClosed().pipe(map((leave) => leave === true));
    }

    protected onSave(): void {
        this.submitted.set(true);
        if (this.form.invalid || this.saving()) {
            return;
        }

        this.saving.set(true);
        const raw = this.form.getRawValue();

        this.surveyService.saveMySurvey({
            ...raw,
            // An empty comment is stored as absent rather than as an empty string, so a query for
            // "responses with a comment" means what it says.
            improvement: raw.improvement?.trim() || undefined,
        } as SurveyDTO)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.form.markAsPristine();
                    this.messages.updateSuccess(UNIHEALTH_CONSTANTS.ENTITY.SURVEY);
                    void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_HOME]);
                },
                error: (err) => {
                    this.saving.set(false);
                    this.messages.error(errorMessageFromHttp(err, this.translate));
                },
            });
    }

    protected onCancel(): void {
        void this.router.navigate(['/', UNIHEALTH_CONSTANTS.ROUTE_HOME]);
    }
}

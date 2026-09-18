import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatDialog} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {finalize, forkJoin, of} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AnalyticsConsentService} from '../../core/services/analytics-consent.service';
import {LabelUsage, UsageTrackingService} from '../../core/services/usage-tracking.service';
import {MatchReasons} from '../../shared/components/match-reasons/match-reasons';
import {SuggestionHero} from '../../shared/components/suggestion-hero/suggestion-hero';
import {TopicSection} from '../health-topics/topic-section/topic-section';
import {HealthProfileViewDTO} from '../../shared/interfaces/health-profile';
import {OptionalHealthProfile} from '../../shared/interfaces/optional-health-profile';
import {UserProfileLabel} from '../../shared/interfaces/user-profile-label';
import {HealthProfileService} from '../../shared/services/health-profile.service';
import {OptionalHealthProfileService} from '../../shared/services/optional-health-profile.service';
import {labelPriorityMap, scoreAgainstLabels} from '../../shared/utils/label-match.util';
import {AdviceTipDialog} from './advice-tip-dialog/advice-tip.dialog';
import {AdviceSection, AdviceSectionView, AdviceTip, AdviceTipView} from './advice-tips.model';
import {AdviceTipsService} from './advice-tips.service';
import {
    AttentionRow,
    buildAttentionComparison,
    buildLifestyleMetrics,
    buildSnapshot,
    LifestyleMetric,
} from './profile-stats.util';

/** How many suggestions the hero shows, matching the topics page. */
const SUGGESTION_COUNT = 3;

/**
 * Advice & tips: suggestions ranked from the user's profiling labels, followed by statistics
 * derived from their own profile.
 *
 * Every figure in the statistics section traces back to a real answer the user gave — see
 * `profile-stats.util.ts`. There is deliberately no composite score and no comparison against
 * other users.
 */
@Component({
    selector: 'app-advice-tips',
    standalone: true,
    imports: [
        MatIconModule,
        TranslatePipe,
        SuggestionHero,
        MatchReasons,
        TopicSection,
    ],
    templateUrl: './advice-tips.html',
    styleUrl: './advice-tips.scss',
})
export class AdviceTips implements OnInit {

    private readonly destroyRef = inject(DestroyRef);
    private readonly dialog = inject(MatDialog);
    private readonly translate = inject(TranslateService);
    private readonly adviceService = inject(AdviceTipsService);
    private readonly healthProfileService = inject(HealthProfileService);
    private readonly optionalProfileService = inject(OptionalHealthProfileService);
    private readonly usageTracking = inject(UsageTrackingService);
    private readonly consent = inject(AnalyticsConsentService);

    private readonly sections = signal<AdviceSection[]>([]);
    private readonly userLabels = signal<UserProfileLabel[]>([]);
    private readonly profile = signal<HealthProfileViewDTO | null>(null);
    private readonly optionalProfile = signal<OptionalHealthProfile | null>(null);
    private readonly usage = signal<LabelUsage[]>([]);

    readonly loading = signal(true);
    readonly loadError = signal(false);
    readonly labelsError = signal(false);

    private readonly priorityMap = computed(() => labelPriorityMap(this.userLabels()));

    /** Sections that match something in this user's profile, best first. */
    private readonly scoredSections = computed<AdviceSectionView[]>(() => {
        const priorities = this.priorityMap();
        if (priorities.size === 0) {
            return [];
        }

        return this.sections()
            .map(section => {
                const match = scoreAgainstLabels(section, priorities);
                return {...section, score: match.score, matchedUserLabels: match.matchedLabels};
            })
            .filter(section => section.score > 0)
            .sort((a, b) => b.score - a.score);
    });

    /** One lead tip per matching section, so the hero spans themes instead of repeating one. */
    private readonly personalisedTips = computed<AdviceTipView[]>(() =>
        this.scoredSections()
            .slice(0, SUGGESTION_COUNT)
            .flatMap(section => section.tips.length
                ? [{
                    ...section.tips[0],
                    icon: section.icon,
                    accentColor: section.accentColor,
                    sectionId: section.id,
                    sectionTitle: section.title,
                    matchedUserLabels: section.matchedUserLabels,
                }]
                : []));

    /**
     * Shown when personalisation has nothing to work with: a new account, a profile never
     * completed, or a failed labels request. The hero must never be empty.
     */
    private readonly fallbackTips = computed<AdviceTipView[]>(() =>
        [...this.sections()]
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .slice(0, SUGGESTION_COUNT)
            .flatMap(section => section.tips.length
                ? [{
                    ...section.tips[0],
                    icon: section.icon,
                    accentColor: section.accentColor,
                    sectionId: section.id,
                    sectionTitle: section.title,
                    matchedUserLabels: [],
                }]
                : []));

    readonly isPersonalised = computed(() => this.personalisedTips().length > 0);

    readonly heroTips = computed(() =>
        this.isPersonalised() ? this.personalisedTips() : this.fallbackTips());

    readonly matchedSignalCount = computed(() =>
        new Set(this.heroTips().flatMap(tip => tip.matchedUserLabels)).size);

    /**
     * Everything that matched, for the browsable list under the statistics.
     *
     * Explicitly typed so the unscored fallback has to be widened into a view rather than leaking a
     * bare `AdviceSection` into the template through a union.
     */
    readonly relevantSections = computed<AdviceSectionView[]>(() => {
        const scored = this.scoredSections();
        if (scored.length) {
            return scored;
        }

        return [...this.sections()]
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map(section => ({...section, score: 0, matchedUserLabels: []}));
    });

    // --- Statistics, all derived from the user's own answers ------------------

    readonly snapshot = computed(() =>
        buildSnapshot(this.profile(), this.optionalProfile(), this.userLabels()));

    readonly lifestyleMetrics = computed<LifestyleMetric[]>(() =>
        buildLifestyleMetrics(this.optionalProfile()));

    /**
     * Profile need against measured engagement, per health area.
     *
     * Replaces the old focus-area chart: that chart drew the same summed priorities this does, so
     * rendering both would show one dataset twice. When no seconds have been recorded the card
     * falls back to the need side alone rather than disappearing.
     */
    private readonly attention = computed(() =>
        buildAttentionComparison(this.userLabels(), this.usage()));

    readonly attentionRows = computed<AttentionRow[]>(() => this.attention().rows);

    readonly hasEngagement = computed(() => this.attention().hasEngagement);

    readonly hiddenAreaCount = computed(() => this.attention().hiddenCount);

    /** Drives which empty-state line shows: "turn it on" versus "nothing yet". */
    readonly consentGranted = this.consent.granted;

    readonly hasStatistics = computed(() =>
        this.snapshot().signalCount > 0
        || this.lifestyleMetrics().length > 0
        || this.snapshot().bmi !== null);

    /**
     * Status tone for the delta figure. A signed difference is a *state*, not a series identity,
     * so it takes the status palette rather than either bar colour.
     */
    deltaTone(row: AttentionRow): string {
        if (row.deltaPoints > 0) {
            return 'over';
        }
        return row.deltaPoints < 0 ? 'under' : 'even';
    }

    ngOnInit(): void {
        this.loadContent();
        this.loadProfile();
    }

    openTip(tip: AdviceTipView): void {
        // A hero tip carries only the labels this user matched; that is the right set to measure.
        this.openDialog(tip, tip.icon, tip.sectionTitle, tip.matchedUserLabels);
    }

    openSectionTip(tip: AdviceTip, section: AdviceSection): void {
        this.openDialog(tip, section.icon, section.title, section.matchedLabels);
    }

    /**
     * `sectionLabels` is threaded through because a tip has no labels of its own — they belong to
     * the section — and the dialog needs them to record what was read.
     */
    private openDialog(
        tip: AdviceTip,
        icon: string,
        sectionTitle: string,
        sectionLabels: string[],
    ): void {
        this.dialog.open(AdviceTipDialog, {
            width: '540px',
            maxWidth: '94vw',
            panelClass: 'advice-dialog',
            data: {...tip, icon, sectionTitle, sectionLabels},
        });
    }

    private loadContent(): void {
        this.adviceService.getSections().pipe(
            takeUntilDestroyed(this.destroyRef),
            finalize(() => this.loading.set(false)),
        ).subscribe({
            next: sections => this.sections.set(sections),
            error: error => {
                console.error('Failed to load advice sections', error);
                this.loadError.set(true);
            },
        });
    }

    /**
     * One pass for the four profile reads. Each falls back to an empty value rather than failing
     * the set, so a user who has not filled in the optional form still gets the rest of the page.
     */
    private loadProfile(): void {
        forkJoin({
            labels: this.healthProfileService.getMyLabels().pipe(catchError(error => {
                console.error('Failed to load profile labels', error);
                this.labelsError.set(true);
                return of<UserProfileLabel[]>([]);
            })),
            profile: this.healthProfileService.getMyProfile().pipe(
                catchError(() => of(null))),
            optional: this.optionalProfileService.getMyOptionalProfile().pipe(
                catchError(() => of(null))),
            // A failed usage read degrades to the need-only card rather than breaking the page.
            usage: this.usageTracking.getMyUsage().pipe(
                catchError(() => of<LabelUsage[]>([]))),
        }).pipe(
            takeUntilDestroyed(this.destroyRef),
        ).subscribe(({labels, profile, optional, usage}) => {
            this.userLabels.set(labels);
            this.profile.set(profile);
            this.optionalProfile.set(optional);
            this.usage.set(usage);
        });
    }
}

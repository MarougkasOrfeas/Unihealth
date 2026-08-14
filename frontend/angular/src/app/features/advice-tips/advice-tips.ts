import {Component, computed, signal} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {
    ADVICE_INSIGHTS,
    ADVICE_SECTIONS,
    AdviceSection,
    AdviceTip,
    AdviceTipView,
} from "./advice-tips.mock";
import {AdviceTipDialog} from "./advice-tip-dialog/advice-tip.dialog";
import {ChartConfiguration} from "chart.js";
import {BaseChartDirective, provideCharts, withDefaultRegisterables} from "ng2-charts";
import {HealthProfileService} from "../../shared/services/health-profile.service";
import {UserProfileLabel} from "../../shared/interfaces/user-profile-label";

@Component({
    selector: 'app-advice-tips',
    standalone: true,
    imports: [MatCardModule, MatIconModule, MatButtonModule, BaseChartDirective],
    providers: [
        provideCharts(withDefaultRegisterables())
    ],
    templateUrl: './advice-tips.html',
    styleUrl: './advice-tips.scss',
})
export class AdviceTips {
    readonly loadingLabels = signal(true);
    readonly labelsError = signal<string | null>(null);
    private readonly userLabels = signal<UserProfileLabel[]>([]);
    readonly insights = computed(() => ADVICE_INSIGHTS);
    readonly labelPriorityMap = computed(() =>
        new Map(this.userLabels().map(label => [label.code, label.priority]))
    );

    readonly sections = computed(() =>
        ADVICE_SECTIONS
            .map(section => this.toScoredSection(section))
            .filter(section => section.recommendationScore > 0)
            .sort((a, b) => b.recommendationScore - a.recommendationScore)
            .slice(0, 3)
    );

    readonly topTips = computed<AdviceTipView[]>(() =>
        this.sections()
            .map(section => {
                const tip = section.tips[0];

                return {
                    ...tip,
                    icon: section.icon,
                    sectionTitle: section.title,
                    imageClass: section.imageClass,
                    recommendationScore: section.recommendationScore,
                    matchedLabelCount: section.matchedLabelCount,
                };
            })
            .slice(0, 3)
    );

    constructor(
        private readonly dialog: MatDialog,
        private readonly healthProfileService: HealthProfileService
    ) {
        this.healthProfileService.getMyLabels().subscribe({
            next: labels => {
                this.userLabels.set(labels);
                this.loadingLabels.set(false);
            },
            error: error => {
                console.error('Failed to load profile labels', error);
                this.labelsError.set('Personalized labels could not be loaded.');
                this.loadingLabels.set(false);
            },
        });
    }

    private toScoredSection(section: AdviceSection): AdviceSection & {
        recommendationScore: number;
        matchedLabelCount: number;
    } {
        const priorityMap = this.labelPriorityMap();
        const matchedLabels = section.matchedLabels.filter(label => priorityMap.has(label));

        const weightedMatchScore = matchedLabels.reduce((score, label) => {
            const priority = priorityMap.get(label) ?? 0;
            const weight = section.labelWeights[label] ?? 1;
            return score + priority * weight;
        }, 0);

        const preferenceBoost = (section.preferenceBoostLabels ?? [])
            .filter(label => priorityMap.has(label))
            .length * 100;

        const safetyPenalty = (section.safetyPenaltyLabels ?? [])
            .filter(label => priorityMap.has(label))
            .length * 150;

        return {
            ...section,
            recommendationScore: Math.max(0, Math.round(weightedMatchScore + preferenceBoost - safetyPenalty)),
            matchedLabelCount: matchedLabels.length,
        };
    }

    openTip(tip: AdviceTip, icon: string, sectionTitle: string): void {
        this.dialog.open(AdviceTipDialog, {
            width: '540px',
            maxWidth: '94vw',
            panelClass: 'advice-dialog',
            data: {
                ...tip,
                icon,
                sectionTitle,
            },
        });
    }

    openTopTip(tip: AdviceTipView): void {
        this.dialog.open(AdviceTipDialog, {
            width: '540px',
            maxWidth: '94vw',
            panelClass: 'advice-dialog',
            data: tip,
        });
    }

    readonly insightChartData: ChartConfiguration<'bar'>['data'] = {
        labels: [
            'Nutrition',
            'Weight',
            'Sleep',
            'Consistency'
        ],
        datasets: [
            {
                label: 'Focus score',
                data: [85, 78, 72, 64],
                borderRadius: 10,
            },
        ],
    };

    readonly insightChartOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                display: false,
            },
            tooltip: {
                callbacks: {
                    label: context => `${context.parsed.y}%`,
                },
            },
        },
        scales: {
            y: {
                min: 0,
                max: 100,
                ticks: {
                    callback: value => `${value}%`,
                },
            },
        },
    };

    readonly recommendationMixChartData: ChartConfiguration<'doughnut'>['data'] = {
        labels: [
            'Nutrition & allergies',
            'Healthy weight',
            'Sleep habits'
        ],
        datasets: [
            {
                data: [40, 35, 25],
            },
        ],
    };

    readonly recommendationMixChartOptions: ChartConfiguration<'doughnut'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'bottom',
            },
            tooltip: {
                callbacks: {
                    label: context => `${context.label}: ${context.parsed}%`,
                },
            },
        },
    };
}

import {Component, computed, signal} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {
    ADVICE_INSIGHTS,
    ADVICE_SECTIONS,
    AdviceTip,
    AdviceTipView,
    MOCK_USER_LABELS,
    UserLabel
} from "./advice-tips.mock";
import {AdviceTipDialog} from "./advice-tip-dialog/advice-tip.dialog";
import {ChartConfiguration} from "chart.js";
import {BaseChartDirective, provideCharts, withDefaultRegisterables} from "ng2-charts";

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
    private readonly userLabels = signal(MOCK_USER_LABELS);
    readonly insights = computed(() => ADVICE_INSIGHTS);

    readonly sections = computed(() =>
        ADVICE_SECTIONS.filter(section =>
            section.matchedLabels.some(label => this.userLabels().includes(label))
        ).slice(0, 3)
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
                };
            })
            .slice(0, 3)
    );

    constructor(private readonly dialog: MatDialog) {
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

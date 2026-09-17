import {
    Component,
    Input,
    inject,
    AfterViewInit,
    ViewChildren,
    QueryList,
    ElementRef,
    ViewChild,
    ChangeDetectorRef, OnInit, HostListener, OnChanges, SimpleChanges, OnDestroy
} from '@angular/core';
import {MatTooltipModule} from '@angular/material/tooltip';
import {Chip} from '../chip/chip';
import {DialogService} from '../../services/dialog.service';
import {LangChangeEvent, TranslatePipe, TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs/internal/Subscription';
import {ListDialogComponent, ListDialogData} from "../list-dialog/list-dialog";

@Component({
    selector: 'app-view-more-list',
    standalone: true,
    imports: [MatTooltipModule, TranslatePipe, Chip],
    templateUrl: './view-more-list.html',
    styleUrl: './view-more-list.scss',
})
export class ViewMoreListComponent implements OnInit, AfterViewInit, OnDestroy {
    private readonly dialogService = inject(DialogService);
    private readonly translateService = inject(TranslateService);
    private readonly cdr = inject(ChangeDetectorRef);
    private _visibleItems: string[] = [];
    private subscription!: Subscription;

    @HostListener('window:resize')
    onResize(): void {
        this._visibleItems = this.safeItems;
        this.cdr.detectChanges();
        requestAnimationFrame(() => {
            this.calculateDisplayedElements();
        });
    }

    @ViewChildren('measureItems')
    measureItems!: QueryList<ElementRef<HTMLSpanElement>>;
    @ViewChild('container')
    container!: ElementRef<HTMLDivElement>;

    @Input() title = '';
    @Input() items: readonly string[] | string | null | undefined = [];
    @Input() moreText = this.translateService.instant('global.more');
    @Input() qlackTranslate: boolean | undefined = false;
    /** Renders each item as a chip instead of as part of a comma-separated text. */
    @Input() chips: boolean | undefined = false;

    private visibleItemsCount = 0;
    private initItems: readonly string[] | string | null | undefined = [];
    @Input() mapItems!: ((row: any) => string[]) | undefined;

    get safeItems(): string[] {
        if (typeof this.initItems === 'string') {
            return this.initItems
                .split(',')
                .map((item) => item.trim())
                .filter(Boolean);
        }

        return (this.initItems ?? []).filter((item): item is string => !!item);
    }

    ngOnInit() {
        this.initValues();
        this.subscription = this.translateService.onLangChange.subscribe(() => {
            this.initValues();
            this.calculateDisplayedElements();
        });
    }

    ngAfterViewInit() {
        requestAnimationFrame(() => {
            this.calculateDisplayedElements();
            this.cdr.detectChanges();
        });
    }

    private initValues() {
        if (this.mapItems) {
            this.initItems = this.mapItems(this.items);
        } else {
            this.initItems = this.items;
        }
        this._visibleItems = this.safeItems;
    }

    private calculateDisplayedElements() {
        this.visibleItemsCount = 0;
        const td = this.container.nativeElement.closest('td') as HTMLElement;
        const availableWidth = td.getBoundingClientRect().width;
        const widths = this.measureItems.map((x) => x.nativeElement.getBoundingClientRect().width);
        let usedWidth = 0;

        for (const width of widths) {
            if (usedWidth + width > availableWidth) {
                break;
            }
            usedWidth += width;
            this.visibleItemsCount++;
        }
        this._visibleItems = this.safeItems.slice(0, this.visibleItemsCount);
        this.cdr.detectChanges();
    }

    get visibleItems(): string[] {
        return this._visibleItems;
    }

    get hiddenItemsCount(): number {
        return Math.max(this.safeItems.length - this.visibleItemsCount, 0);
    }

    get tooltipText(): string {
        return this.safeItems.join(', ');
    }

    hasComma(index: number): boolean {
        return index < this.visibleItems.length - 1;
    }

    openDialog(event?: Event): void {
        event?.stopPropagation();
        let itemsToShow = this.safeItems;
        if (this.qlackTranslate) {
            itemsToShow = this.safeItems.map(si => this.translateService.instant(si));
        }
        this.dialogService.open<ListDialogComponent, ListDialogData>(ListDialogComponent,
            {
                width: '480px',
                data: {
                    title: this.title,
                    items: itemsToShow,
                },
            },
        );
    }

    onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            event.stopPropagation();
            this.openDialog();
        }
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}

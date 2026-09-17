import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {TranslateModule} from '@ngx-translate/core';
import {of} from 'rxjs';

import {BaseTable, TableColumnData} from './base-table';
import {BaseEntity} from '../../interfaces/baseEntity';
import {BaseService} from '../../services/base.service';
import {Page} from '../../interfaces/page';

interface TestDto extends BaseEntity {
    name: string;
    active: boolean;
}

type TestRow = { id: string; name: string; active: boolean };

/**
 * Stands in for a real service. The table takes its service as an input rather than through an
 * injection token, so a plain object is enough — no DI wiring, no HTTP double.
 */
function fakeService(page: Partial<Page<TestDto>> = {}): BaseService<TestDto> {
    return {
        getPage: () => of({content: [], totalElements: 0, size: 10, number: 0, ...page}),
        getFacetOptions: () => of([]),
        getExport: () => of({blob: new Blob(), filename: 'x.xlsx'}),
    } as unknown as BaseService<TestDto>;
}

const COLUMNS: TableColumnData<TestRow>[] = [
    {key: 'name', label: 'test.column.name', sortable: true, filterable: true},
    {
        key: 'active',
        label: 'test.column.active',
        cellType: 'status',
        status: (row) => row.active
            ? {tone: 'active', labelKey: 'global.status.active'}
            : {tone: 'inactive', labelKey: 'global.status.inactive'},
    },
];

describe('BaseTable', () => {
    let fixture: ComponentFixture<BaseTable<TestDto, TestRow>>;

    async function render(service: BaseService<TestDto>) {
        await TestBed.configureTestingModule({
            imports: [BaseTable, TranslateModule.forRoot()],
            providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
        }).compileComponents();

        fixture = TestBed.createComponent<BaseTable<TestDto, TestRow>>(BaseTable);
        fixture.componentRef.setInput('service', service);
        fixture.componentRef.setInput('columns', COLUMNS);
        fixture.componentRef.setInput('pageName', 'test');
        fixture.componentRef.setInput(
            'mapToRow',
            (dto: TestDto): TestRow => ({id: dto.id, name: dto.name, active: dto.active}),
        );
        fixture.detectChanges();
    }

    it('creates and loads its first page', async () => {
        await render(fakeService({
            content: [{id: '1', name: 'Alpha', active: true}],
            totalElements: 1,
        }));

        expect(fixture.componentInstance).toBeTruthy();
        expect(fixture.nativeElement.textContent).toContain('Alpha');
    });

    it('renders the empty state when there are no rows', async () => {
        await render(fakeService());

        expect(fixture.nativeElement.querySelector('[data-testid="test-no-data-row"]')).toBeTruthy();
    });
});

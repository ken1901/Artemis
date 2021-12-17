import { EventEmitter, SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { ArtemisTestModule } from '../../test.module';
import { MockTranslateService } from '../../helpers/mocks/service/mock-translate.service';
import { SplitPaneHeaderComponent } from 'app/exercises/shared/plagiarism/plagiarism-split-view/split-pane-header/split-pane-header.component';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { NgModel } from '@angular/forms';
import { PlagiarismDetailsComponent } from 'app/exercises/shared/plagiarism/plagiarism-details/plagiarism-details.component';
import { PlagiarismRunDetailsComponent } from 'app/exercises/shared/plagiarism/plagiarism-run-details/plagiarism-run-details.component';
import { PlagiarismSidebarComponent } from 'app/exercises/shared/plagiarism/plagiarism-sidebar/plagiarism-sidebar.component';
import { MockPipe, MockDirective, MockComponent } from 'ng-mocks';

describe('Plagiarism Split Pane Header Component', () => {
    let comp: SplitPaneHeaderComponent;
    let fixture: ComponentFixture<SplitPaneHeaderComponent>;

    const files = ['src/Main.java', 'src/Utils.java', 'src/Other.java'];

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule],
            declarations: [
                SplitPaneHeaderComponent,
                MockPipe(ArtemisTranslatePipe),
                MockDirective(NgbTooltip),
                MockDirective(NgModel),
                MockComponent(PlagiarismDetailsComponent),
                MockComponent(PlagiarismRunDetailsComponent),
                MockComponent(PlagiarismSidebarComponent),
            ],
            providers: [{ provide: TranslateService, useClass: MockTranslateService }],
            teardown: { destroyAfterEach: true },
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(SplitPaneHeaderComponent);
                comp = fixture.componentInstance;

                comp.files = [];
                comp.studentLogin = 'ts10abc';
                comp.selectFile = new EventEmitter<string>();
            });
    });

    it('resets the active file index on change', () => {
        comp.activeFileIndex = 1;

        comp.ngOnChanges({
            files: { currentValue: files } as SimpleChange,
        });

        expect(comp.activeFileIndex).toEqual(0);
    });

    it('selects the first file on change', () => {
        comp.files = files;
        jest.spyOn(comp.selectFile, 'emit');

        comp.ngOnChanges({
            files: { currentValue: files } as SimpleChange,
        });

        expect(comp.selectFile.emit).toHaveBeenCalledTimes(1);
        expect(comp.selectFile.emit).toHaveBeenCalledWith(files[0]);
    });

    it('does not find an active file', () => {
        const activeFile = comp.getActiveFile();

        expect(activeFile).toBe(false);
    });

    it('returns the active file', () => {
        comp.files = files;
        const activeFile = comp.getActiveFile();

        expect(activeFile).toBe(files[0]);
    });

    it('handles selection of a file', () => {
        const idx = 1;
        comp.showFiles = true;
        jest.spyOn(comp.selectFile, 'emit');

        comp.handleFileSelect(files[idx], idx);

        expect(comp.activeFileIndex).toBe(idx);
        expect(comp.showFiles).toBe(false);
        expect(comp.selectFile.emit).toHaveBeenCalledTimes(1);
        expect(comp.selectFile.emit).toHaveBeenCalledWith(files[idx]);
    });

    it('has no files', () => {
        expect(comp.hasFiles()).toBe(false);
    });

    it('has files', () => {
        comp.files = files;

        expect(comp.hasFiles()).toBe(true);
    });

    it('toggles "show files"', () => {
        comp.showFiles = false;
        comp.files = files;

        comp.toggleShowFiles();

        expect(comp.showFiles).toBe(true);
    });

    it('does not toggle "show files"', () => {
        comp.showFiles = false;

        comp.toggleShowFiles();

        expect(comp.showFiles).toBe(false);
    });
});

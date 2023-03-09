import { NgModule } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { TestwiseCoverageFileComponent } from 'app/exercises/programming/hestia/testwise-coverage-report/testwise-coverage-file.component';
import { TestwiseCoverageReportModalComponent } from 'app/exercises/programming/hestia/testwise-coverage-report/testwise-coverage-report-modal.component';
import { TestwiseCoverageReportComponent } from 'app/exercises/programming/hestia/testwise-coverage-report/testwise-coverage-report.component';
import { AceEditorModule } from 'app/shared/markdown-editor/ace-editor/ace-editor.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [ArtemisSharedModule, AceEditorModule, MatExpansionModule],
    declarations: [TestwiseCoverageFileComponent, TestwiseCoverageReportComponent, TestwiseCoverageReportModalComponent],
    exports: [TestwiseCoverageFileComponent, TestwiseCoverageReportModalComponent, TestwiseCoverageReportComponent],
})
export class TestwiseCoverageReportModule {}

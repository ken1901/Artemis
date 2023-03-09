import { NgModule } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { RouterModule } from '@angular/router';
import { CodeHintGenerationOverviewComponent } from 'app/exercises/programming/hestia/generation-overview/code-hint-generation-overview/code-hint-generation-overview.component';
import { CodeHintGenerationStatusStepComponent } from 'app/exercises/programming/hestia/generation-overview/code-hint-generation-status/code-hint-generation-status-step.component';
import { GitDiffReportModule } from 'app/exercises/programming/hestia/git-diff-report/git-diff-report.module';
import { TestwiseCoverageReportModule } from 'app/exercises/programming/hestia/testwise-coverage-report/testwise-coverage-report.module';
import { ArtemisExerciseHintSharedModule } from 'app/exercises/shared/exercise-hint/shared/exercise-hint-shared.module';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';
import { ArtemisMarkdownModule } from 'app/shared/markdown.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { CodeHintGenerationStatusComponent } from '../code-hint-generation-status/code-hint-generation-status.component';
import { ManualSolutionEntryCreationModalComponent } from '../manual-solution-entry-creation-modal/manual-solution-entry-creation-modal.component';
import { SolutionEntryDetailsModalComponent } from '../solution-entry-details-modal/solution-entry-details-modal.component';
import { CodeHintGenerationStepComponent } from '../steps/code-hint-generation-step/code-hint-generation-step.component';
import { CoverageGenerationStepComponent } from '../steps/coverage-generation-step/coverage-generation-step.component';
import { DiffGenerationStepComponent } from '../steps/diff-generation-step/diff-generation-step.component';
import { SolutionEntryGenerationStepComponent } from '../steps/solution-entry-generation-step/solution-entry-generation-step.component';

@NgModule({
    imports: [
        RouterModule,
        ArtemisSharedModule,
        ArtemisExerciseHintSharedModule,
        ArtemisSharedComponentModule,
        TestwiseCoverageReportModule,
        GitDiffReportModule,
        GitDiffReportModule,
        TestwiseCoverageReportModule,
        ArtemisSharedComponentModule,
        ArtemisMarkdownModule,
        ArtemisExerciseHintSharedModule,
        MatExpansionModule,
    ],
    declarations: [
        CodeHintGenerationStatusComponent,
        SolutionEntryDetailsModalComponent,
        DiffGenerationStepComponent,
        CoverageGenerationStepComponent,
        SolutionEntryGenerationStepComponent,
        CodeHintGenerationStepComponent,
        CodeHintGenerationOverviewComponent,
        CodeHintGenerationStatusStepComponent,
        ManualSolutionEntryCreationModalComponent,
    ],
    exports: [
        CodeHintGenerationStatusComponent,
        SolutionEntryDetailsModalComponent,
        DiffGenerationStepComponent,
        CoverageGenerationStepComponent,
        SolutionEntryGenerationStepComponent,
        CodeHintGenerationStepComponent,
        CodeHintGenerationOverviewComponent,
        CodeHintGenerationStatusStepComponent,
    ],
})
export class ArtemisCodeHintGenerationOverviewModule {}

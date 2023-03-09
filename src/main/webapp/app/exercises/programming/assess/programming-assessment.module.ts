import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssessmentInstructionsModule } from 'app/assessment/assessment-instructions/assessment-instructions.module';
import { ArtemisAssessmentSharedModule } from 'app/assessment/assessment-shared.module';
import { ArtemisComplaintsForTutorModule } from 'app/complaints/complaints-for-tutor/complaints-for-tutor.module';
import { CodeEditorTutorAssessmentContainerComponent } from 'app/exercises/programming/assess/code-editor-tutor-assessment-container.component';
import { ArtemisProgrammingAssessmentDashboardModule } from 'app/exercises/programming/assess/programming-assessment-dashboard/programming-exercises-submissions.module';
import { ArtemisProgrammingAssessmentRoutingModule } from 'app/exercises/programming/assess/programming-assessment.route';
import { ArtemisProgrammingManualAssessmentModule } from 'app/exercises/programming/assess/programming-manual-assessment.module';
import { ProgrammingAssessmentRepoExportButtonComponent } from 'app/exercises/programming/assess/repo-export/programming-assessment-repo-export-button.component';
import { ProgrammingAssessmentRepoExportDialogComponent } from 'app/exercises/programming/assess/repo-export/programming-assessment-repo-export-dialog.component';
import { ArtemisCodeEditorModule } from 'app/exercises/programming/shared/code-editor/code-editor.module';
import { ArtemisProgrammingExerciseInstructionsRenderModule } from 'app/exercises/programming/shared/instructions-render/programming-exercise-instructions-render.module';
import { ArtemisHeaderExercisePageWithDetailsModule } from 'app/exercises/shared/exercise-headers/exercise-headers.module';
import { ArtemisResultModule } from 'app/exercises/shared/result/result.module';
import { OrionTutorAssessmentComponent } from 'app/orion/assessment/orion-tutor-assessment.component';
import { SubmissionResultStatusModule } from 'app/overview/submission-result-status.module';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { FeatureToggleModule } from 'app/shared/feature-toggle/feature-toggle.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [
        ArtemisSharedModule,
        ArtemisSharedComponentModule,
        FormDateTimePickerModule,
        FormsModule,
        FeatureToggleModule,
        ArtemisComplaintsForTutorModule,
        ArtemisProgrammingAssessmentRoutingModule,
        ArtemisAssessmentSharedModule,
        ArtemisCodeEditorModule,
        ArtemisResultModule,
        ArtemisProgrammingExerciseInstructionsRenderModule,
        ArtemisProgrammingManualAssessmentModule,
        AssessmentInstructionsModule,
        ArtemisHeaderExercisePageWithDetailsModule,
        ArtemisProgrammingAssessmentDashboardModule,
        SubmissionResultStatusModule,
    ],
    declarations: [
        ProgrammingAssessmentRepoExportButtonComponent,
        ProgrammingAssessmentRepoExportDialogComponent,
        CodeEditorTutorAssessmentContainerComponent,
        OrionTutorAssessmentComponent,
    ],
    exports: [ProgrammingAssessmentRepoExportButtonComponent],
})
export class ArtemisProgrammingAssessmentModule {}

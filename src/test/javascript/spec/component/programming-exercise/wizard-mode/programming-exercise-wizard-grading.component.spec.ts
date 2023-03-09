import { ComponentFixture, TestBed, fakeAsync } from '@angular/core/testing';
import { DefaultValueAccessor, NgModel } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AssessmentType } from 'app/entities/assessment-type.model';
import { IncludedInOverallScore } from 'app/entities/exercise.model';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { SubmissionPolicyType } from 'app/entities/submission-policy.model';
import { ProgrammingExerciseGradingComponent } from 'app/exercises/programming/manage/update/update-components/programming-exercise-grading.component';
import { ProgrammingExerciseUpdateWizardGradingComponent } from 'app/exercises/programming/manage/update/wizard-mode/programming-exercise-update-wizard-grading.component';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { RemoveKeysPipe } from 'app/shared/pipes/remove-keys.pipe';
import { MockComponent, MockPipe } from 'ng-mocks';
import { of } from 'rxjs';
import { MockTranslateService } from '../../../helpers/mocks/service/mock-translate.service';

describe('ProgrammingExerciseWizardGradingComponent', () => {
    let wizardComponentFixture: ComponentFixture<ProgrammingExerciseUpdateWizardGradingComponent>;
    let wizardComponent: ProgrammingExerciseUpdateWizardGradingComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            declarations: [
                ProgrammingExerciseUpdateWizardGradingComponent,
                MockPipe(ArtemisTranslatePipe),
                MockComponent(ProgrammingExerciseGradingComponent),
                DefaultValueAccessor,
                NgModel,
                MockPipe(RemoveKeysPipe),
            ],
            providers: [
                { provide: TranslateService, useClass: MockTranslateService },
                {
                    provide: ActivatedRoute,
                    useValue: { queryParams: of({}) },
                },
            ],
            schemas: [],
        })
            .compileComponents()
            .then(() => {
                wizardComponentFixture = TestBed.createComponent(ProgrammingExerciseUpdateWizardGradingComponent);
                wizardComponent = wizardComponentFixture.componentInstance;

                const exercise = new ProgrammingExercise(undefined, undefined);
                exercise.maxPoints = 10;
                exercise.includedInOverallScore = IncludedInOverallScore.INCLUDED_COMPLETELY;
                exercise.assessmentType = AssessmentType.AUTOMATIC;
                exercise.submissionPolicy = { type: SubmissionPolicyType.NONE };

                wizardComponent.programmingExercise = exercise;
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should initialize', fakeAsync(() => {
        wizardComponentFixture.detectChanges();
        expect(wizardComponent).not.toBeNull();
    }));
});

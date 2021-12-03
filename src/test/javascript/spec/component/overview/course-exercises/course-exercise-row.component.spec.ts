import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { ParticipationWebsocketService } from 'app/overview/participation-websocket.service';
import { ArtemisTestModule } from '../../../test.module';
import { TranslateModule } from '@ngx-translate/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { MockParticipationWebsocketService } from '../../../helpers/mocks/service/mock-participation-websocket.service';
import { Result } from 'app/entities/result.model';
import { AccountService } from 'app/core/auth/account.service';
import { MockAccountService } from '../../../helpers/mocks/service/mock-account.service';
import dayjs from 'dayjs/esm';
import { Exercise, ExerciseType, ParticipationStatus } from 'app/entities/exercise.model';
import { InitializationState } from 'app/entities/participation/participation.model';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { CourseExerciseRowComponent } from 'app/overview/course-exercises/course-exercise-row.component';
import { QuizExercise } from 'app/entities/quiz/quiz-exercise.model';
import { Course } from 'app/entities/course.model';
import { MockComponent, MockDirective, MockPipe } from 'ng-mocks';
import { SubmissionResultStatusComponent } from 'app/overview/submission-result-status.component';
import { ExerciseDetailsStudentActionsComponent } from 'app/overview/exercise-details/exercise-details-student-actions.component';
import { NotReleasedTagComponent } from 'app/shared/components/not-released-tag.component';
import { DifficultyBadgeComponent } from 'app/exercises/shared/exercise-headers/difficulty-badge.component';
import { IncludedInScoreBadgeComponent } from 'app/exercises/shared/exercise-headers/included-in-score-badge.component';
import { ArtemisTimeAgoPipe } from 'app/shared/pipes/artemis-time-ago.pipe';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { OrionFilterDirective } from 'app/shared/orion/orion-filter.directive';
import { MockParticipationService } from '../../../helpers/mocks/service/mock-participation.service';
import { ParticipationService } from 'app/exercises/shared/participation/participation.service';
import { MockExerciseService } from '../../../helpers/mocks/service/mock-exercise.service';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';

describe('CourseExerciseRowComponent', () => {
    let comp: CourseExerciseRowComponent;
    let fixture: ComponentFixture<CourseExerciseRowComponent>;
    let debugElement: DebugElement;
    let getAllParticipationsStub: jest.SpyInstance;
    let participationWebsocketService: ParticipationWebsocketService;

    beforeAll(() => {
        return TestBed.configureTestingModule({
            imports: [ArtemisTestModule, TranslateModule.forRoot()],
            declarations: [
                CourseExerciseRowComponent,
                MockComponent(SubmissionResultStatusComponent),
                MockComponent(ExerciseDetailsStudentActionsComponent),
                MockComponent(NotReleasedTagComponent),
                MockComponent(DifficultyBadgeComponent),
                MockComponent(IncludedInScoreBadgeComponent),
                MockDirective(NgbTooltip),
                MockPipe(ArtemisTimeAgoPipe),
                MockPipe(ArtemisTranslatePipe),
                MockDirective(OrionFilterDirective),
            ],
            providers: [
                { provide: AccountService, useClass: MockAccountService },
                { provide: ParticipationService, useClass: MockParticipationService },
                { provide: ExerciseService, useClass: MockExerciseService },
                { provide: ParticipationWebsocketService, useClass: MockParticipationWebsocketService },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(CourseExerciseRowComponent);
                comp = fixture.componentInstance;
                comp.course = { id: 123, isAtLeastInstructor: true } as Course;
                debugElement = fixture.debugElement;
                participationWebsocketService = debugElement.injector.get(ParticipationWebsocketService);
                getAllParticipationsStub = jest.spyOn(participationWebsocketService, 'getParticipationForExercise');
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_NOT_STARTED if release date is in the past and not planned to start', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(false, dayjs(), dayjs().subtract(3, 'minutes'), true, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_NOT_STARTED);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_NOT_STARTED if release date is in the future and planned to start', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs(), dayjs().add(3, 'minutes'), true, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_NOT_STARTED);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_UNINITIALIZED', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs().add(3, 'minutes'), dayjs(), true, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_UNINITIALIZED);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_NOT_PARTICIPATED if there are no participations', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs(), dayjs(), true, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_NOT_PARTICIPATED);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_ACTIVE', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs().add(3, 'minutes'), dayjs(), true, true, InitializationState.INITIALIZED);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_ACTIVE);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_SUBMITTED', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs().add(3, 'minutes'), dayjs(), true, true, InitializationState.FINISHED);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_SUBMITTED);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_NOT_PARTICIPATED if there are no results', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs().add(3, 'minutes'), dayjs(), false, true, InitializationState.UNINITIALIZED, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_NOT_PARTICIPATED);
    });

    it('Participation status of quiz exercise should evaluate to QUIZ_FINISHED', () => {
        setupForTestingParticipationStatusExerciseTypeQuiz(true, dayjs().add(3, 'minutes'), dayjs(), false, true, InitializationState.UNINITIALIZED, true);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.QUIZ_FINISHED);
    });

    it('Participation status of text exercise should evaluate to EXERCISE_ACTIVE', () => {
        setupForTestingParticipationStatusExerciseTypeText(ExerciseType.TEXT, InitializationState.INITIALIZED, true);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.EXERCISE_ACTIVE);
    });

    it('Participation status of text exercise should evaluate to EXERCISE_MISSED', () => {
        setupForTestingParticipationStatusExerciseTypeText(ExerciseType.TEXT, InitializationState.INITIALIZED, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.EXERCISE_MISSED);
    });

    it('Participation status of text exercise should evaluate to EXERCISE_SUBMITTED', () => {
        setupForTestingParticipationStatusExerciseTypeText(ExerciseType.TEXT, InitializationState.FINISHED, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.EXERCISE_SUBMITTED);
    });

    it('Participation status of text exercise should evaluate to UNINITIALIZED', () => {
        setupForTestingParticipationStatusExerciseTypeText(ExerciseType.TEXT, InitializationState.UNINITIALIZED, false);
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.UNINITIALIZED);
    });

    it('Participation status of programming exercise should evaluate to EXERCISE_MISSED', () => {
        setupExercise(ExerciseType.PROGRAMMING, dayjs().subtract(1, 'day'));
        comp.ngOnInit();
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.EXERCISE_MISSED);
    });

    it('Participation status of programming exercise should evaluate to UNINITIALIZED', () => {
        setupExercise(ExerciseType.PROGRAMMING, dayjs().add(1, 'day'));
        comp.ngOnInit();
        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.UNINITIALIZED);
    });

    it('Participation status of programming exercise should evaluate to INITIALIZED', () => {
        setupExercise(ExerciseType.PROGRAMMING, dayjs());

        const studentParticipation = {
            id: 1,
            initializationState: InitializationState.INITIALIZED,
        } as StudentParticipation;
        comp.exercise.studentParticipations = [studentParticipation];

        getAllParticipationsStub.mockReturnValue(studentParticipation);
        comp.ngOnInit();

        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.INITIALIZED);
    });

    it('Participation status of programming exercise should evaluate to EXERCISE_MISSED', () => {
        setupExercise(ExerciseType.PROGRAMMING, dayjs().subtract(1, 'day'));

        const studentParticipation = {
            id: 1,
            initializationState: InitializationState.UNINITIALIZED,
        } as StudentParticipation;
        comp.exercise.studentParticipations = [studentParticipation];

        getAllParticipationsStub.mockReturnValue(studentParticipation);
        comp.ngOnInit();

        expect(comp.exercise.participationStatus).toBe(ParticipationStatus.EXERCISE_MISSED);
    });

    const setupForTestingParticipationStatusExerciseTypeQuiz = (
        isPlannedToStart: boolean,
        dueDate: dayjs.Dayjs,
        releaseDate: dayjs.Dayjs,
        visibleToStudents: boolean,
        hasParticipations: boolean,
        initializationState?: InitializationState,
        hasResults?: boolean,
    ) => {
        comp.exercise = {
            id: 1,
            dueDate,
            isPlannedToStart,
            releaseDate,
            type: ExerciseType.QUIZ,
            visibleToStudents,
        } as QuizExercise;

        if (hasParticipations) {
            const studentParticipation = {
                id: 1,
                initializationState,
            } as StudentParticipation;

            if (hasResults) {
                studentParticipation.results = [{ id: 1 } as Result];
            }

            comp.exercise.studentParticipations = [studentParticipation];

            getAllParticipationsStub.mockReturnValue(studentParticipation);
        }

        comp.ngOnInit();
    };

    const setupForTestingParticipationStatusExerciseTypeText = (exerciseType: ExerciseType, initializationState: InitializationState, inDueDate: boolean) => {
        const dueDate = inDueDate ? dayjs().add(3, 'days') : dayjs().subtract(3, 'days');
        setupExercise(exerciseType, dueDate);

        const studentParticipation = {
            id: 1,
            initializationState,
        } as StudentParticipation;
        comp.exercise.studentParticipations = [studentParticipation];

        getAllParticipationsStub.mockReturnValue(studentParticipation);
        comp.ngOnInit();
    };

    const setupExercise = (exerciseType: ExerciseType, dueDate: dayjs.Dayjs) => {
        comp.exercise = {
            id: 1,
            type: exerciseType,
            dueDate,
        } as Exercise;
    };
});

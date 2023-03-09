import { HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AlertService } from 'app/core/util/alert.service';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { TutorialGroupsConfigurationService } from 'app/course/tutorial-groups/services/tutorial-groups-configuration.service';
import { EditTutorialGroupsConfigurationComponent } from 'app/course/tutorial-groups/tutorial-groups-management/tutorial-groups-configuration/crud/edit-tutorial-groups-configuration/edit-tutorial-groups-configuration.component';
import { Course } from 'app/entities/course.model';
import { TutorialGroupsConfiguration } from 'app/entities/tutorial-group/tutorial-groups-configuration.model';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { MockPipe, MockProvider } from 'ng-mocks';
import { of } from 'rxjs';
import { mockedActivatedRoute } from '../../../../../helpers/mocks/activated-route/mock-activated-route-query-param-map';
import { MockRouter } from '../../../../../helpers/mocks/mock-router';
import { LoadingIndicatorContainerStubComponent } from '../../../../../helpers/stubs/loading-indicator-container-stub.component';
import { generateExampleTutorialGroupsConfiguration, tutorialsGroupsConfigurationToFormData } from '../../../helpers/tutorialGroupsConfigurationExampleModels';
import { TutorialGroupsConfigurationFormStubComponent } from '../../../stubs/tutorial-groups-configuration-form-sub.component';

describe('EditTutorialGroupsConfigurationComponent', () => {
    let fixture: ComponentFixture<EditTutorialGroupsConfigurationComponent>;
    let component: EditTutorialGroupsConfigurationComponent;
    let configurationService: TutorialGroupsConfigurationService;
    let courseManagementService: CourseManagementService;
    let findConfigurationSpy: jest.SpyInstance;
    let exampleConfiguration: TutorialGroupsConfiguration;
    const course = { id: 1, title: 'Example' } as Course;
    const router = new MockRouter();

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                EditTutorialGroupsConfigurationComponent,
                LoadingIndicatorContainerStubComponent,
                TutorialGroupsConfigurationFormStubComponent,
                MockPipe(ArtemisTranslatePipe),
            ],
            providers: [
                MockProvider(TutorialGroupsConfigurationService),
                MockProvider(AlertService),
                MockProvider(CourseManagementService),
                { provide: Router, useValue: router },
                mockedActivatedRoute(
                    {
                        tutorialGroupsConfigurationId: 2,
                    },
                    {},
                    { course },
                    {},
                ),
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(EditTutorialGroupsConfigurationComponent);
                component = fixture.componentInstance;
                configurationService = TestBed.inject(TutorialGroupsConfigurationService);

                exampleConfiguration = generateExampleTutorialGroupsConfiguration({ id: 2 });
                course.tutorialGroupsConfiguration = exampleConfiguration;
                courseManagementService = TestBed.inject(CourseManagementService);

                findConfigurationSpy = jest.spyOn(configurationService, 'getOneOfCourse').mockReturnValue(of(new HttpResponse({ body: exampleConfiguration })));
                fixture.detectChanges();
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should initialize', () => {
        expect(component).not.toBeNull();
        expect(findConfigurationSpy).toHaveBeenCalledOnce();
        expect(findConfigurationSpy).toHaveBeenCalledWith(course.id);
    });

    it('should set form data correctly', () => {
        const formStub: TutorialGroupsConfigurationFormStubComponent = fixture.debugElement.query(By.directive(TutorialGroupsConfigurationFormStubComponent)).componentInstance;

        expect(component.tutorialGroupsConfiguration).toEqual(exampleConfiguration);
        expect(findConfigurationSpy).toHaveBeenCalledOnce();
        expect(findConfigurationSpy).toHaveBeenCalledWith(course.id);

        expect(component.formData.period).toEqual([exampleConfiguration.tutorialPeriodStartInclusive?.toDate(), exampleConfiguration.tutorialPeriodEndInclusive?.toDate()]);
        expect(formStub.formData).toEqual(component.formData);
    });

    it('should send PUT request upon form submission and navigate', () => {
        const changedConfiguration: TutorialGroupsConfiguration = {
            ...exampleConfiguration,
        };

        const updateResponse: HttpResponse<TutorialGroupsConfiguration> = new HttpResponse({
            body: changedConfiguration,
            status: 200,
        });

        const updateCourseSpy = jest.spyOn(courseManagementService, 'courseWasUpdated');
        const updatedStub = jest.spyOn(configurationService, 'update').mockReturnValue(of(updateResponse));
        const navigateSpy = jest.spyOn(router, 'navigate');

        const sessionForm: TutorialGroupsConfigurationFormStubComponent = fixture.debugElement.query(By.directive(TutorialGroupsConfigurationFormStubComponent)).componentInstance;

        const formData = tutorialsGroupsConfigurationToFormData(changedConfiguration);

        sessionForm.formSubmitted.emit(formData);
        expect(updatedStub).toHaveBeenCalledOnce();
        expect(updatedStub).toHaveBeenCalledWith(course.id, exampleConfiguration.id, changedConfiguration, formData.period);
        expect(navigateSpy).toHaveBeenCalledOnce();
        expect(navigateSpy).toHaveBeenCalledWith(['/course-management', course.id, 'tutorial-groups']);
        expect(updateCourseSpy).toHaveBeenCalledOnce();
        expect(updateCourseSpy).toHaveBeenCalledWith(component.course);
    });
});

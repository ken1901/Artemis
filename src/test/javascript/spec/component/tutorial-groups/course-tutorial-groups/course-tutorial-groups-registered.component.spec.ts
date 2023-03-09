import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Course } from 'app/entities/course.model';
import { TutorialGroup } from 'app/entities/tutorial-group/tutorial-group.model';
import { CourseTutorialGroupsRegisteredComponent } from 'app/overview/course-tutorial-groups/course-tutorial-groups-registered/course-tutorial-groups-registered.component';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { MockPipe } from 'ng-mocks';
import { generateExampleTutorialGroup } from '../helpers/tutorialGroupExampleModels';

@Component({ selector: 'jhi-course-tutorial-group-card', template: '' })
class MockCourseTutorialGroupCardComponent {
    @Input()
    course: Course;
    @Input()
    tutorialGroup: TutorialGroup;

    @Input()
    showChannelLink = false;
}

describe('CourseTutorialGroupsRegisteredComponent', () => {
    let component: CourseTutorialGroupsRegisteredComponent;
    let fixture: ComponentFixture<CourseTutorialGroupsRegisteredComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [CourseTutorialGroupsRegisteredComponent, MockCourseTutorialGroupCardComponent, MockPipe(ArtemisTranslatePipe)],
        }).compileComponents();

        fixture = TestBed.createComponent(CourseTutorialGroupsRegisteredComponent);
        component = fixture.componentInstance;
        component.course = { id: 1, postsEnabled: true } as Course;
        component.registeredTutorialGroups = [generateExampleTutorialGroup({})];
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

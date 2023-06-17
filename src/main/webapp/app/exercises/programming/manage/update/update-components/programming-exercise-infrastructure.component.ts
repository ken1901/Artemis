import { Component, Input } from '@angular/core';
import { faQuestionCircle } from '@fortawesome/free-solid-svg-icons';
import { ProgrammingExercise, ProjectType } from 'app/entities/programming-exercise.model';
import { InfrastructureStepInputs } from '../wizard-mode/programming-exercise-update-wizard.component';

@Component({
    selector: 'jhi-programming-exercise-infrastructure',
    templateUrl: './programming-exercise-infrastructure.component.html',
    styleUrls: ['../../programming-exercise-form.scss'],
})
export class ProgrammingExerciseInfrastructureComponent {
    readonly ProjectType = ProjectType;

    @Input() programmingExercise: ProgrammingExercise;
    @Input() infrastructureStepInputs: InfrastructureStepInputs;

    faQuestionCircle = faQuestionCircle;
}

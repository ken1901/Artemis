import { Component, Input, OnInit } from '@angular/core';
import { UMLModel } from '@ls1intum/apollon';
import { ModelingExercise } from 'app/entities/modeling-exercise.model';
import { ModelingSubmission } from 'app/entities/modeling-submission.model';

@Component({
    selector: 'jhi-modeling-exam-summary',
    templateUrl: './modeling-exam-summary.component.html',
})
export class ModelingExamSummaryComponent implements OnInit {
    @Input()
    exercise: ModelingExercise;

    @Input()
    submission: ModelingSubmission;

    umlModel: UMLModel;
    explanation: string;

    constructor() {}

    ngOnInit() {
        if (this.submission) {
            if (this.submission.model) {
                this.umlModel = JSON.parse(this.submission.model);
            }
            if (this.submission.explanationText) {
                this.explanation = this.submission.explanationText;
            }
        }
    }
}

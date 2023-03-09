import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { faListAlt } from '@fortawesome/free-regular-svg-icons';
import { faBook, faPlus, faSort, faTable, faTimes, faUsers, faWrench } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { AccountService } from 'app/core/auth/account.service';
import { AlertService } from 'app/core/util/alert.service';
import { EventManager } from 'app/core/util/event-manager.service';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { ExerciseType } from 'app/entities/exercise.model';
import { FileUploadExercise } from 'app/entities/file-upload-exercise.model';
import { CourseExerciseService } from 'app/exercises/shared/course-exercises/course-exercise.service';
import { ExerciseComponent } from 'app/exercises/shared/exercise/exercise.component';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { ExerciseImportComponent } from 'app/exercises/shared/import/exercise-import.component';
import { SortService } from 'app/shared/service/sort.service';
import { onError } from 'app/shared/util/global.utils';
import { filter } from 'rxjs/operators';
import { FileUploadExerciseService } from './file-upload-exercise.service';

@Component({
    selector: 'jhi-file-upload-exercise',
    templateUrl: './file-upload-exercise.component.html',
})
export class FileUploadExerciseComponent extends ExerciseComponent {
    @Input() fileUploadExercises: FileUploadExercise[] = [];
    filteredFileUploadExercises: FileUploadExercise[] = [];

    // Icons
    faSort = faSort;
    faPlus = faPlus;
    faTimes = faTimes;
    faBook = faBook;
    faWrench = faWrench;
    faUsers = faUsers;
    faTable = faTable;
    farListAlt = faListAlt;

    constructor(
        public exerciseService: ExerciseService,
        private fileUploadExerciseService: FileUploadExerciseService,
        private courseExerciseService: CourseExerciseService,
        private alertService: AlertService,
        private accountService: AccountService,
        private modalService: NgbModal,
        private router: Router,
        private sortService: SortService,
        courseService: CourseManagementService,
        translateService: TranslateService,
        eventManager: EventManager,
        route: ActivatedRoute,
    ) {
        super(courseService, translateService, route, eventManager);
    }

    protected loadExercises(): void {
        this.courseExerciseService
            .findAllFileUploadExercisesForCourse(this.courseId)
            .pipe(filter((res) => !!res.body))
            .subscribe({
                next: (res: HttpResponse<FileUploadExercise[]>) => {
                    this.fileUploadExercises = res.body!;
                    // reconnect exercise with course
                    this.fileUploadExercises.forEach((exercise) => {
                        exercise.course = this.course;
                        this.accountService.setAccessRightsForExercise(exercise);
                    });
                    this.emitExerciseCount(this.fileUploadExercises.length);
                    this.applyFilter();
                },
                error: (res: HttpErrorResponse) => onError(this.alertService, res),
            });
    }

    protected applyFilter(): void {
        this.filteredFileUploadExercises = this.fileUploadExercises.filter((exercise) => this.filter.matchesExercise(exercise));
        this.emitFilteredExerciseCount(this.filteredFileUploadExercises.length);
    }

    /**
     * Returns the unique identifier for items in the collection
     * @param index of a file upload exercise in the collection
     * @param item current file upload exercise
     */
    trackId(index: number, item: FileUploadExercise) {
        return item.id;
    }

    /**
     * Deletes file upload exercise
     * @param fileUploadExerciseId id of the exercise that will be deleted
     */
    deleteFileUploadExercise(fileUploadExerciseId: number) {
        this.fileUploadExerciseService.delete(fileUploadExerciseId).subscribe({
            next: () => {
                this.eventManager.broadcast({
                    name: 'fileUploadExerciseListModification',
                    content: 'Deleted an fileUploadExercise',
                });
                this.dialogErrorSource.next('');
            },
            error: (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        });
    }

    protected getChangeEventName(): string {
        return 'fileUploadExerciseListModification';
    }

    sortRows() {
        this.sortService.sortByProperty(this.fileUploadExercises, this.predicate, this.reverse);
        this.applyFilter();
    }

    /**
     * Used in the template for jhiSort
     */
    callback() {}

    openImportModal() {
        const modalRef = this.modalService.open(ExerciseImportComponent, { size: 'lg', backdrop: 'static' });
        modalRef.componentInstance.exerciseType = ExerciseType.FILE_UPLOAD;
        modalRef.result.then(
            (result: FileUploadExercise) => {
                this.router.navigate(['course-management', this.courseId, 'file-upload-exercises', result.id, 'import']);
            },
            () => {},
        );
    }
}

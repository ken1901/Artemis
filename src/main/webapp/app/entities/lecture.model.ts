import { Attachment } from 'app/entities/attachment.model';
import { Course } from 'app/entities/course.model';
import { LectureUnit } from 'app/entities/lecture-unit/lectureUnit.model';
import { Post } from 'app/entities/metis/post.model';
import { BaseEntity } from 'app/shared/model/base-entity';
import dayjs from 'dayjs/esm';

export class Lecture implements BaseEntity {
    id?: number;
    title?: string;
    description?: string;
    startDate?: dayjs.Dayjs;
    endDate?: dayjs.Dayjs;
    attachments?: Attachment[];
    posts?: Post[];
    lectureUnits?: LectureUnit[];
    course?: Course;

    // helper attribute
    isAtLeastEditor?: boolean;
    isAtLeastInstructor?: boolean;

    constructor() {}
}

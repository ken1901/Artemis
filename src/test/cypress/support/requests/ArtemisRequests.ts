import { CourseManagementRequests } from './CourseManagementRequests';
import { ExamBuilder } from './ExamBuilder';

/**
 * A class which encapsulates all cypress requests, which can be sent to Artemis.
 */
export class ArtemisRequests {
    courseManagement = new CourseManagementRequests();
    examBuilder = new ExamBuilder(undefined);
}

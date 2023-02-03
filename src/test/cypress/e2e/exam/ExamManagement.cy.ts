import { Interception } from 'cypress/types/net-stubbing';
import { Exam } from 'app/entities/exam.model';
import { Course } from 'app/entities/course.model';
import { CypressExamBuilder, convertCourseAfterMultiPart } from '../../support/requests/CourseManagementRequests';
import { artemis } from '../../support/ArtemisTesting';
import { generateUUID } from '../../support/utils';

// Users
const users = artemis.users;
const admin = users.getAdmin();
const studentOne = users.getStudentOne();

// Requests
const courseManagementRequest = artemis.requests.courseManagement;

// PageObjects
const navigationBar = artemis.pageObjects.navigationBar;
const courseManagement = artemis.pageObjects.course.management;
const examManagement = artemis.pageObjects.exam.management;
const textCreation = artemis.pageObjects.exercise.text.creation;
const programmingCreation = artemis.pageObjects.exercise.programming.creation;
const quizCreation = artemis.pageObjects.exercise.quiz.creation;
const modelingCreation = artemis.pageObjects.exercise.modeling.creation;
const exerciseGroups = artemis.pageObjects.exam.exerciseGroups;
const exerciseGroupCreation = artemis.pageObjects.exam.exerciseGroupCreation;
const studentExamManagement = artemis.pageObjects.exam.studentExamManagement;

// Common primitives
const uid = generateUUID();
const examTitle = 'exam' + uid;
let groupCount = 0;

describe('Exam management', () => {
    let course: Course;
    let exam: Exam;

    before(() => {
        cy.login(admin);
        courseManagementRequest.createCourse(true).then((response) => {
            course = convertCourseAfterMultiPart(response);
            courseManagementRequest.addStudentToCourse(course, studentOne);
            const examConfig = new CypressExamBuilder(course).title(examTitle).build();
            courseManagementRequest.createExam(examConfig).then((examResponse) => {
                exam = examResponse.body;
            });
        });
    });

    beforeEach(() => {
        cy.login(admin);
    });

    it('Create exercise group', () => {
        cy.visit('/');
        navigationBar.openCourseManagement();
        courseManagement.openExamsOfCourse(course.shortName!);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.shouldShowNumberOfExerciseGroups(groupCount);
        exerciseGroups.clickAddExerciseGroup();
        const groupName = 'Group 1';
        exerciseGroupCreation.typeTitle(groupName);
        exerciseGroupCreation.isMandatoryBoxShouldBeChecked();
        exerciseGroupCreation.clickSave();
        groupCount++;
        exerciseGroups.shouldHaveTitle(groupCount - 1, groupName);
        exerciseGroups.shouldShowNumberOfExerciseGroups(groupCount);
    });

    it('Adds a text exercise', () => {
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.clickAddTextExercise(0);
        const textExerciseTitle = 'text' + uid;
        textCreation.typeTitle(textExerciseTitle);
        textCreation.typeMaxPoints(10);
        textCreation.create().its('response.statusCode').should('eq', 201);
        exerciseGroups.visitPageViaUrl(course.id!, exam.id!);
        exerciseGroups.shouldContainExerciseWithTitle(textExerciseTitle);
    });

    it('Adds a quiz exercise', () => {
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.clickAddQuizExercise(0);
        const quizExerciseTitle = 'quiz' + uid;
        quizCreation.setTitle(quizExerciseTitle);
        quizCreation.addMultipleChoiceQuestion(quizExerciseTitle, 10);
        quizCreation.saveQuiz().its('response.statusCode').should('eq', 201);
        exerciseGroups.visitPageViaUrl(course.id!, exam.id!);
        exerciseGroups.shouldContainExerciseWithTitle(quizExerciseTitle);
    });

    it('Adds a modeling exercise', () => {
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.clickAddModelingExercise(0);
        const modelingExerciseTitle = 'modeling' + uid;
        modelingCreation.setTitle(modelingExerciseTitle);
        modelingCreation.setPoints(10);
        modelingCreation.save().its('response.statusCode').should('eq', 201);
        exerciseGroups.visitPageViaUrl(course.id!, exam.id!);
        exerciseGroups.shouldContainExerciseWithTitle(modelingExerciseTitle);
    });

    it('Adds a programming exercise', () => {
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.clickAddProgrammingExercise(0);
        const programmingExerciseTitle = 'programming' + uid;
        programmingCreation.setTitle(programmingExerciseTitle);
        programmingCreation.setShortName(programmingExerciseTitle);
        programmingCreation.setPackageName('de.test');
        programmingCreation.setPoints(10);
        programmingCreation.generate().its('response.statusCode').should('eq', 201);
        exerciseGroups.visitPageViaUrl(course.id!, exam.id!);
        exerciseGroups.shouldContainExerciseWithTitle(programmingExerciseTitle);
    });

    it('Edits an exercise group', () => {
        cy.visit('/');
        navigationBar.openCourseManagement();
        courseManagement.openExamsOfCourse(course.shortName!);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.shouldShowNumberOfExerciseGroups(groupCount);
        exerciseGroups.clickAddExerciseGroup();
        const groupName = 'Group 2';
        exerciseGroupCreation.typeTitle(groupName);
        exerciseGroupCreation.isMandatoryBoxShouldBeChecked();
        exerciseGroupCreation.clickSave();
        groupCount++;

        exerciseGroups.shouldHaveTitle(groupCount - 1, groupName);
        exerciseGroups.clickEditGroup(groupCount - 1);
        const newGroupName = 'Group 3';
        exerciseGroupCreation.typeTitle(newGroupName);
        exerciseGroupCreation.update();
        exerciseGroups.shouldHaveTitle(groupCount - 1, newGroupName);
    });

    it('Delete an exercise group', () => {
        cy.visit('/');
        navigationBar.openCourseManagement();
        courseManagement.openExamsOfCourse(course.shortName!);
        examManagement.openExerciseGroups(exam.id!);
        exerciseGroups.clickDeleteGroup(groupCount - 1, 'Group 3');
    });

    it('Registers the course students for the exam', () => {
        // We already verified in the previous test that we can navigate here
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.openStudentRegistration(exam.id!);
        studentExamManagement.clickRegisterCourseStudents().then((request: Interception) => {
            expect(request.response!.statusCode).to.eq(200);
        });
        cy.get('#registered-students').contains(studentOne.username).should('be.visible');
    });

    it('Generates student exams', () => {
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.openStudentExams(exam.id!);
        studentExamManagement.clickGenerateStudentExams();
        cy.get('#generateMissingStudentExamsButton').should('be.disabled');
    });

    after(() => {
        if (course) {
            cy.login(admin);
            courseManagementRequest.deleteCourse(course.id!);
        }
    });
});

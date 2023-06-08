package de.tum.in.www1.artemis.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.quiz.DragAndDropQuestion;
import de.tum.in.www1.artemis.domain.quiz.MultipleChoiceQuestion;
import de.tum.in.www1.artemis.domain.quiz.QuizGroup;
import de.tum.in.www1.artemis.domain.quiz.QuizPool;
import de.tum.in.www1.artemis.domain.quiz.QuizQuestion;
import de.tum.in.www1.artemis.domain.quiz.ShortAnswerQuestion;
import de.tum.in.www1.artemis.service.QuizPoolService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.RequestUtilService;

class QuizPoolIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "quizpoolintegration";

    @Autowired
    private QuizPoolService quizPoolService;

    @Autowired
    private DatabaseUtilService database;

    @Autowired
    private RequestUtilService request;

    private Course course;

    private Exam exam;

    private QuizPool quizPool;

    @BeforeEach
    void initTestCase() {
        database.addUsers(TEST_PREFIX, 0, 0, 0, 1);
        course = database.addEmptyCourse();
        User instructor = database.getUserByLogin(TEST_PREFIX + "instructor1");
        instructor.setGroups(Set.of(course.getInstructorGroupName()));
        exam = database.addExam(course);
        quizPool = quizPoolService.update(exam.getId(), new QuizPool());
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateQuizPoolSuccessful() throws Exception {
        QuizGroup quizGroup0 = database.createQuizGroup("Encapsulation");
        QuizGroup quizGroup1 = database.createQuizGroup("Inheritance");
        QuizGroup quizGroup2 = database.createQuizGroup("Polymorphism");
        QuizQuestion mcQuizQuestion0 = database.createMultipleChoiceQuestionWithTitleAndGroup("MC 0", quizGroup0);
        QuizQuestion mcQuizQuestion1 = database.createMultipleChoiceQuestionWithTitleAndGroup("MC 1", quizGroup0);
        QuizQuestion dndQuizQuestion0 = database.createDragAndDropQuestionWithTitleAndGroup("DND 0", quizGroup1);
        QuizQuestion dndQuizQuestion1 = database.createDragAndDropQuestionWithTitleAndGroup("DND 1", quizGroup2);
        QuizQuestion saQuizQuestion0 = database.createShortAnswerQuestionWithTitleAndGroup("SA 0", null);
        quizPool.setQuizGroups(List.of(quizGroup0, quizGroup1, quizGroup2));
        quizPool.setQuizQuestions(List.of(mcQuizQuestion0, mcQuizQuestion1, dndQuizQuestion0, dndQuizQuestion1, saQuizQuestion0));

        QuizPool responseQuizPool = request.putWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/quiz-pools", quizPool, QuizPool.class,
                HttpStatus.OK, null);

        assertThat(responseQuizPool.getExam().getId()).isEqualTo(exam.getId());
        assertThat(responseQuizPool.getQuizGroups()).hasSize(quizPool.getQuizGroups().size()).extracting("name").containsExactly(quizPool.getQuizGroups().get(0).getName(),
                quizPool.getQuizGroups().get(1).getName(), quizPool.getQuizGroups().get(2).getName());
        assertThat(responseQuizPool.getQuizQuestions()).hasSize(quizPool.getQuizQuestions().size()).extracting("title", "quizGroup.name").containsExactly(
                tuple(quizPool.getQuizQuestions().get(0).getTitle(), quizGroup0.getName()), tuple(quizPool.getQuizQuestions().get(1).getTitle(), quizGroup0.getName()),
                tuple(quizPool.getQuizQuestions().get(2).getTitle(), quizGroup1.getName()), tuple(quizPool.getQuizQuestions().get(3).getTitle(), quizGroup2.getName()),
                tuple(quizPool.getQuizQuestions().get(4).getTitle(), null));

        QuizGroup quizGroup3 = database.createQuizGroup("Exception Handling");
        QuizQuestion saQuizQuestion1 = database.createShortAnswerQuestionWithTitleAndGroup("SA 1", quizGroup2);
        QuizQuestion saQuizQuestion2 = database.createShortAnswerQuestionWithTitleAndGroup("SA 2", quizGroup3);
        QuizQuestion saQuizQuestion3 = database.createShortAnswerQuestionWithTitleAndGroup("SA 3", null);
        responseQuizPool.setQuizGroups(List.of(responseQuizPool.getQuizGroups().get(0), responseQuizPool.getQuizGroups().get(2), quizGroup3));
        responseQuizPool.setQuizQuestions(List.of(responseQuizPool.getQuizQuestions().get(0), responseQuizPool.getQuizQuestions().get(1),
                responseQuizPool.getQuizQuestions().get(2), saQuizQuestion1, saQuizQuestion2, saQuizQuestion3));
        responseQuizPool.getQuizQuestions().get(2).setQuizGroup(quizGroup3);

        QuizPool responseQuizPool2 = request.putWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/quiz-pools", responseQuizPool, QuizPool.class,
                HttpStatus.OK, null);

        assertThat(responseQuizPool2.getExam().getId()).isEqualTo(exam.getId());
        assertThat(responseQuizPool2.getQuizGroups()).hasSize(responseQuizPool.getQuizGroups().size()).extracting("name").containsExactly(
                responseQuizPool.getQuizGroups().get(0).getName(), responseQuizPool.getQuizGroups().get(1).getName(), responseQuizPool.getQuizGroups().get(2).getName());
        assertThat(responseQuizPool2.getQuizQuestions()).hasSize(responseQuizPool.getQuizQuestions().size()).extracting("title", "quizGroup.name").containsExactly(
                tuple(responseQuizPool.getQuizQuestions().get(0).getTitle(), quizGroup0.getName()),
                tuple(responseQuizPool.getQuizQuestions().get(1).getTitle(), quizGroup0.getName()),
                tuple(responseQuizPool.getQuizQuestions().get(2).getTitle(), quizGroup3.getName()),
                tuple(responseQuizPool.getQuizQuestions().get(3).getTitle(), quizGroup2.getName()),
                tuple(responseQuizPool.getQuizQuestions().get(4).getTitle(), quizGroup3.getName()), tuple(responseQuizPool.getQuizQuestions().get(5).getTitle(), null));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateQuizPoolBadRequestInvalidMCQuestion() throws Exception {
        MultipleChoiceQuestion quizQuestion = database.createMultipleChoiceQuestion();
        quizQuestion.setTitle(null);
        quizPool.setQuizQuestions(List.of(quizQuestion));

        request.putWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/quiz-pools", quizPool, QuizPool.class, HttpStatus.BAD_REQUEST, null);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateQuizPoolBadRequestInvalidDnDQuestion() throws Exception {
        DragAndDropQuestion quizQuestion = database.createDragAndDropQuestion();
        quizQuestion.setCorrectMappings(null);
        quizPool.setQuizQuestions(List.of(quizQuestion));

        request.putWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/quiz-pools", quizPool, QuizPool.class, HttpStatus.BAD_REQUEST, null);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateQuizPoolBadRequestInvalidSAQuestion() throws Exception {
        ShortAnswerQuestion quizQuestion = database.createShortAnswerQuestion();
        quizQuestion.setCorrectMappings(null);
        quizPool.setQuizQuestions(List.of(quizQuestion));

        request.putWithResponseBody("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/quiz-pools", quizPool, QuizPool.class, HttpStatus.BAD_REQUEST, null);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateQuizPoolNotFoundCourse() throws Exception {
        QuizQuestion quizQuestion = database.createMultipleChoiceQuestion();
        quizQuestion.setTitle(null);
        quizPool.setQuizQuestions(List.of(quizQuestion));

        int notFoundCourseId = 0;
        request.putWithResponseBody("/api/courses/" + notFoundCourseId + "/exams/" + exam.getId() + "/quiz-pools", quizPool, QuizPool.class, HttpStatus.NOT_FOUND, null);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testUpdateQuizPoolNotFoundExam() throws Exception {
        QuizQuestion quizQuestion = database.createMultipleChoiceQuestion();
        quizQuestion.setTitle(null);
        quizPool.setQuizQuestions(List.of(quizQuestion));

        int notFoundExamId = 0;
        request.putWithResponseBody("/api/courses/" + course.getId() + "/exams/" + notFoundExamId + "/quiz-pools", quizPool, QuizPool.class, HttpStatus.NOT_FOUND, null);
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testGetQuizPoolSuccessful() throws Exception {
        QuizGroup quizGroup0 = database.createQuizGroup("Encapsulation");
        QuizGroup quizGroup1 = database.createQuizGroup("Inheritance");
        QuizQuestion mcQuizQuestion = database.createMultipleChoiceQuestionWithTitleAndGroup("MC", quizGroup0);
        QuizQuestion dndQuizQuestion = database.createDragAndDropQuestionWithTitleAndGroup("DND", quizGroup1);
        QuizQuestion saQuizQuestion = database.createShortAnswerQuestionWithTitleAndGroup("SA", null);
        quizPool.setQuizGroups(List.of(quizGroup0, quizGroup1));
        quizPool.setQuizQuestions(List.of(mcQuizQuestion, dndQuizQuestion, saQuizQuestion));
        QuizPool savedQuizPool = quizPoolService.update(exam.getId(), quizPool);

        QuizPool responseQuizPool = request.get("/api/courses/" + course.getId() + "/exams/" + exam.getId() + "/quiz-pools", HttpStatus.OK, QuizPool.class);
        assertThat(responseQuizPool.getExam().getId()).isEqualTo(exam.getId());
        assertThat(responseQuizPool.getQuizGroups()).hasSize(savedQuizPool.getQuizGroups().size()).containsExactly(savedQuizPool.getQuizGroups().get(0),
                savedQuizPool.getQuizGroups().get(1));
        assertThat(responseQuizPool.getQuizQuestions()).hasSize(savedQuizPool.getQuizQuestions().size()).containsExactly(savedQuizPool.getQuizQuestions().get(0),
                savedQuizPool.getQuizQuestions().get(1), savedQuizPool.getQuizQuestions().get(2));
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void testGetQuizPoolNotFoundExam() throws Exception {
        int notFoundExamId = 0;
        request.get("/api/courses/" + course.getId() + "/exams/" + notFoundExamId + "/quiz-pools", HttpStatus.NOT_FOUND, QuizPool.class);
    }
}

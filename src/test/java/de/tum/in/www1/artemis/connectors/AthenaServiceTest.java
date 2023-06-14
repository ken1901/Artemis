package de.tum.in.www1.artemis.connectors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.connector.AthenaRequestMockProvider;
import de.tum.in.www1.artemis.domain.TextExercise;
import de.tum.in.www1.artemis.domain.TextSubmission;
import de.tum.in.www1.artemis.domain.enumeration.InitializationState;
import de.tum.in.www1.artemis.repository.StudentParticipationRepository;
import de.tum.in.www1.artemis.repository.TextSubmissionRepository;
import de.tum.in.www1.artemis.service.connectors.athena.AthenaService;
import de.tum.in.www1.artemis.util.ModelFactory;

class AthenaServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "athenaservice";

    @Autowired
    private AthenaRequestMockProvider athenaRequestMockProvider;

    @Autowired
    private StudentParticipationRepository participationRepository;

    @Autowired
    private TextSubmissionRepository textSubmissionRepository;

    @Autowired
    private AthenaService athenaService;

    private TextExercise exercise1;

    /**
     * Initializes athenaService and example exercise
     */
    @BeforeEach
    void init() {
        // Create example exercise
        database.addUsers(TEST_PREFIX, 10, 1, 0, 1);
        var course = database.addCourseWithOneReleasedTextExercise();
        exercise1 = (TextExercise) course.getExercises().iterator().next();
        athenaRequestMockProvider.enableMockingOfRequests();
    }

    @AfterEach
    void tearDown() throws Exception {
        athenaRequestMockProvider.reset();
        athenaService.finishTask(exercise1.getId());
    }

    /**
     * Submits a job to athenaService without any submissions
     */
    @Test
    void submitJobWithoutSubmissions() {
        athenaService.submitJob(exercise1);
        assertThat(!athenaService.isTaskRunning(exercise1.getId())).isTrue();
    }

    private List<TextSubmission> generateTextSubmissions(int size) {
        var textSubmissions = ModelFactory.generateTextSubmissions(size);
        for (var i = 0; i < size; i++) {
            var textSubmission = textSubmissions.get(i);
            textSubmission.setId(null);
            var student = database.getUserByLogin(TEST_PREFIX + "student" + (i + 1));
            var participation = ModelFactory.generateStudentParticipation(InitializationState.INITIALIZED, exercise1, student);
            participation = participationRepository.save(participation);
            textSubmission.setParticipation(participation);
            textSubmission.setSubmitted(true);
        }

        return textSubmissionRepository.saveAll(textSubmissions);
    }

    /**
     * Submits a job to athenaService with less than 10 submissions (will use fallback segmentation without athena)
     */
    @Test
    void submitJobWithLessThan10Submissions() {
        generateTextSubmissions(9);
        athenaService.submitJob(exercise1);
        assertThat(athenaService.isTaskRunning(exercise1.getId())).isFalse();
    }

    /**
     * Submits a job to athenaService with 10 submissions (will trigger athena)
     */
    @Test
    void submitJobWith10Submissions() {
        generateTextSubmissions(10);

        // Create mock server
        athenaRequestMockProvider.mockSubmitSubmissions();

        athenaService.submitJob(exercise1);
        assertThat(athenaService.isTaskRunning(exercise1.getId())).isTrue();
    }

}

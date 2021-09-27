package de.tum.in.www1.artemis.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.connectors.LtiService;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

@Service
public class ResultService {

    private final Logger log = LoggerFactory.getLogger(ResultService.class);

    private final UserRepository userRepository;

    private final ResultRepository resultRepository;

    private final LtiService ltiService;

    private final WebsocketMessagingService websocketMessagingService;

    private final ComplaintResponseRepository complaintResponseRepository;

    private final RatingRepository ratingRepository;

    private final FeedbackRepository feedbackRepository;

    private final SubmissionRepository submissionRepository;

    private final ComplaintRepository complaintRepository;

    public ResultService(UserRepository userRepository, ResultRepository resultRepository, LtiService ltiService, FeedbackRepository feedbackRepository,
            WebsocketMessagingService websocketMessagingService, ComplaintResponseRepository complaintResponseRepository, SubmissionRepository submissionRepository,
            ComplaintRepository complaintRepository, RatingRepository ratingRepository) {
        this.userRepository = userRepository;
        this.resultRepository = resultRepository;
        this.ltiService = ltiService;
        this.websocketMessagingService = websocketMessagingService;
        this.feedbackRepository = feedbackRepository;
        this.complaintResponseRepository = complaintResponseRepository;
        this.submissionRepository = submissionRepository;
        this.complaintRepository = complaintRepository;
        this.ratingRepository = ratingRepository;
    }

    /**
     * Orphan submissions are those that are not example submissions and that are not connected to participations
     */
    @PostConstruct
    public void deleteOrphanResults() {
        try {
            var orphanResults = this.resultRepository.findByParticipationIsNullAndSubmissionIsNull();
            log.info("Found {} result orphans to delete", orphanResults.size());
            for (Result result : orphanResults) {
                log.info("Delete orphan result {} with all its related content", result.getId());
                deleteResultWithComplaint(result.getId());
            }
        }
        catch (Exception ex) {
            log.error("Deleting orphans did not work", ex);
        }
    }

    /**
     * Handle the manual creation of a new result potentially including feedback
     *
     * @param result newly created Result
     * @param isProgrammingExerciseWithFeedback defines if the programming exercise contains feedback
     * @param ratedResult override value for rated property of result
     *
     * @return updated result with eagerly loaded Submission and Feedback items.
     */
    public Result createNewManualResult(Result result, boolean isProgrammingExerciseWithFeedback, boolean ratedResult) {
        if (!result.getFeedbacks().isEmpty()) {
            result.setHasFeedback(isProgrammingExerciseWithFeedback);
        }

        User user = userRepository.getUserWithGroupsAndAuthorities();

        result.setAssessmentType(AssessmentType.MANUAL);
        result.setAssessor(user);
        result.setCompletionDate(ZonedDateTime.now());

        // manual feedback is always rated, can be overwritten though in the case of a result for an external submission
        result.setRated(ratedResult);

        result.getFeedbacks().forEach(feedback -> {
            feedback.setResult(result);
        });

        // this call should cascade all feedback relevant changed and save them accordingly
        var savedResult = resultRepository.save(result);
        // The websocket client expects the submission and feedbacks, so we retrieve the result again instead of using the save result.
        savedResult = resultRepository.findOneWithEagerSubmissionAndFeedback(result.getId());

        // if it is an example result we do not have any participation (isExampleResult can be also null)
        if (Boolean.FALSE.equals(savedResult.isExampleResult()) || savedResult.isExampleResult() == null) {

            if (savedResult.getParticipation() instanceof ProgrammingExerciseStudentParticipation) {
                ltiService.onNewResult((StudentParticipation) savedResult.getParticipation());
            }

            websocketMessagingService.broadcastNewResult(savedResult.getParticipation(), savedResult);
        }
        return savedResult;
    }

    public Result createNewRatedManualResult(Result result, boolean isProgrammingExerciseWithFeedback) {
        return createNewManualResult(result, isProgrammingExerciseWithFeedback, true);
    }

    /**
     * NOTE: As we use delete methods with underscores, we need a transactional context here!
     * Deletes result with corresponding complaint and complaint response
     * @param resultId the id of the result
     */
    @Transactional // ok
    public void deleteResultWithComplaint(long resultId) {
        complaintResponseRepository.deleteByComplaint_Result_Id(resultId);
        complaintRepository.deleteByResult_Id(resultId);
        ratingRepository.deleteByResult_Id(resultId);
        resultRepository.deleteById(resultId);
    }

    /**
     * Create a new example result for the provided submission ID.
     *
     * @param submissionId The ID of the submission (that is connected to an example submission) for which a result should get created
     * @param isProgrammingExerciseWithFeedback defines if the programming exercise contains feedback
     * @return The newly created (and empty) example result
     */
    public Result createNewExampleResultForSubmissionWithExampleSubmission(long submissionId, boolean isProgrammingExerciseWithFeedback) {
        final var submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("No example submission with ID " + submissionId + " found!"));
        if (submission.isExampleSubmission() != Boolean.TRUE) {
            throw new IllegalArgumentException("Submission is no example submission! Example results are not allowed!");
        }

        final var newResult = new Result();
        newResult.setSubmission(submission);
        newResult.setExampleResult(true);
        return createNewRatedManualResult(newResult, isProgrammingExerciseWithFeedback);
    }

    /**
     * Store the given feedback to the passed result (by replacing all existing feedback) with a workaround for Hibernate exceptions.
     * <p>
     * With ordered collections (like result and feedback here), we have to be very careful with the way we persist the objects in the database.
     * We must first persist the child object without a relation to the parent object. Then, we recreate the association and persist the parent object.
     *
     * If the result is not saved (shouldSave = false), the caller is responsible to save the result (which will persist the feedback changes as well)
     *
     * @param result           the result with should be saved with the given feedback
     * @param feedbackList     new feedback items which replace the existing feedback
     * @param shouldSave       whether the result should be saved or not
     * @return the updated (and potentially saved) result
     */
    public Result storeFeedbackInResult(@NotNull Result result, List<Feedback> feedbackList, boolean shouldSave) {
        var savedFeedbacks = saveFeedbackWithHibernateWorkaround(result, feedbackList);
        result.setFeedbacks(savedFeedbacks);
        return shouldSaveResult(result, shouldSave);
    }

    /**
     * Add the feedback to the passed result with a workaround for Hibernate exceptions.
     * <p>
     * With ordered collections (like result and feedback here), we have to be very careful with the way we persist the objects in the database.
     * We must first persist the child object without a relation to the parent object. Then, we recreate the association and persist the parent object.
     *
     * If the result is not saved (shouldSave = false), the caller is responsible to save the result (which will persist the feedback changes as well)
     *
     * @param result           the result with should be saved with the given feedback
     * @param feedbackList     new feedback items which should be added to the feedback
     * @param shouldSave       whether the result should be saved or not
     * @return the updated (and potentially saved) result
     */
    @NotNull
    public Result addFeedbackToResult(@NotNull Result result, List<Feedback> feedbackList, boolean shouldSave) {
        List<Feedback> savedFeedbacks = saveFeedbackWithHibernateWorkaround(result, feedbackList);
        result.addFeedbacks(savedFeedbacks);
        return shouldSaveResult(result, shouldSave);
    }

    @NotNull
    private List<Feedback> saveFeedbackWithHibernateWorkaround(@NotNull Result result, List<Feedback> feedbackList) {
        // Avoid hibernate exception
        List<Feedback> savedFeedbacks = new ArrayList<>();
        feedbackList.forEach(feedback -> {
            // cut association to parent object
            feedback.setResult(null);
            // persist the child object without an association to the parent object.
            feedback = feedbackRepository.saveAndFlush(feedback);
            // restore the association to the parent object
            feedback.setResult(result);
            savedFeedbacks.add(feedback);
        });
        return savedFeedbacks;
    }

    @NotNull
    private Result shouldSaveResult(@NotNull Result result, boolean shouldSave) {
        if (shouldSave) {
            // Note: This also saves the feedback objects in the database because of the 'cascade = CascadeType.ALL' option.
            return resultRepository.save(result);
        }
        else {
            return result;
        }
    }
}

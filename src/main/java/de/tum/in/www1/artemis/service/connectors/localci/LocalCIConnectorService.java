package de.tum.in.www1.artemis.service.connectors.localci;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.Commit;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.ProgrammingSubmission;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.domain.participation.SolutionProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.exception.LocalCIException;
import de.tum.in.www1.artemis.exception.localvc.LocalVCInternalException;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseStudentParticipationRepository;
import de.tum.in.www1.artemis.repository.SolutionProgrammingExerciseParticipationRepository;
import de.tum.in.www1.artemis.repository.TemplateProgrammingExerciseParticipationRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.connectors.localci.dto.LocalCIBuildResult;
import de.tum.in.www1.artemis.service.connectors.localvc.LocalVCRepositoryUrl;
import de.tum.in.www1.artemis.service.programming.ProgrammingExerciseGradingService;
import de.tum.in.www1.artemis.service.programming.ProgrammingExerciseParticipationService;
import de.tum.in.www1.artemis.service.programming.ProgrammingMessagingService;
import de.tum.in.www1.artemis.service.programming.ProgrammingSubmissionService;
import de.tum.in.www1.artemis.service.programming.ProgrammingTriggerService;
import de.tum.in.www1.artemis.service.util.TimeLogUtil;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

/**
 * This service connects the local VC system and the online editor to the local CI system.
 * It contains the {@link #processNewPush(String, Repository)} method that is called by the local VC system and the RepositoryResource and makes sure the correct build is
 * triggered.
 */
@Service
@Profile("localci")
public class LocalCIConnectorService {

    private final Logger log = LoggerFactory.getLogger(LocalCIConnectorService.class);

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final ProgrammingSubmissionService programmingSubmissionService;

    private final ProgrammingMessagingService programmingMessagingService;

    private final ProgrammingTriggerService programmingTriggerService;

    private final LocalCIBuildJobExecutionService localCIBuildJobExecutionService;

    private final ProgrammingExerciseGradingService programmingExerciseGradingService;

    private final ProgrammingExerciseParticipationService programmingExerciseParticipationService;

    private final ProgrammingExerciseStudentParticipationRepository studentParticipationRepository;

    private final TemplateProgrammingExerciseParticipationRepository templateParticipationRepository;

    private final SolutionProgrammingExerciseParticipationRepository solutionParticipationRepository;

    private final LocalCITriggerService localCITriggerService;

    private final AuthorizationCheckService authorizationCheckService;

    private final UserRepository userRepository;

    @Value("${artemis.version-control.url}")
    private URL localVCBaseUrl;

    public LocalCIConnectorService(ProgrammingExerciseRepository programmingExerciseRepository, ProgrammingSubmissionService programmingSubmissionService,
            ProgrammingMessagingService programmingMessagingService, ProgrammingTriggerService programmingTriggerService,
            LocalCIBuildJobExecutionService localCIBuildJobExecutionService, ProgrammingExerciseGradingService programmingExerciseGradingService,
            ProgrammingExerciseParticipationService programmingExerciseParticipationService, ProgrammingExerciseStudentParticipationRepository studentParticipationRepository,
            TemplateProgrammingExerciseParticipationRepository templateParticipationRepository, SolutionProgrammingExerciseParticipationRepository solutionParticipationRepository,
            LocalCITriggerService localCITriggerService, AuthorizationCheckService authorizationCheckService, UserRepository userRepository) {
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.programmingSubmissionService = programmingSubmissionService;
        this.programmingMessagingService = programmingMessagingService;
        this.programmingTriggerService = programmingTriggerService;
        this.localCIBuildJobExecutionService = localCIBuildJobExecutionService;
        this.programmingExerciseGradingService = programmingExerciseGradingService;
        this.programmingExerciseParticipationService = programmingExerciseParticipationService;
        this.studentParticipationRepository = studentParticipationRepository;
        this.templateParticipationRepository = templateParticipationRepository;
        this.solutionParticipationRepository = solutionParticipationRepository;
        this.localCITriggerService = localCITriggerService;
        this.authorizationCheckService = authorizationCheckService;
        this.userRepository = userRepository;
    }

    /**
     * Trigger the respective build and process the results.
     *
     * @param commitHash the hash of the last commit.
     * @param repository the remote repository which was pushed to.
     * @throws LocalCIException if something goes wrong preparing the queueing of the build job.
     */
    public void processNewPush(String commitHash, Repository repository) {
        long timeNanoStart = System.nanoTime();

        Path repositoryFolderPath = repository.getDirectory().toPath();

        LocalVCRepositoryUrl localVCRepositoryUrl = getLocalVCRepositoryUrl(repositoryFolderPath);

        String repositoryTypeOrUserName = localVCRepositoryUrl.getRepositoryTypeOrUserName();
        String projectKey = localVCRepositoryUrl.getProjectKey();

        ProgrammingExercise exercise;

        try {
            exercise = programmingExerciseRepository.findOneByProjectKeyOrThrow(projectKey, false);
        }
        catch (EntityNotFoundException e) {
            throw new LocalCIException("Could not find programming exercise for project key " + projectKey, e);
        }

        ProgrammingExerciseParticipation participation;

        try {
            participation = getParticipationForRepository(exercise, repositoryTypeOrUserName, localVCRepositoryUrl.isPracticeRepository(), true);
        }
        catch (EntityNotFoundException e) {
            throw new LocalCIException("Could not find participation for repository " + repositoryTypeOrUserName + " of exercise " + exercise, e);
        }

        try {
            if (commitHash == null) {
                commitHash = getLatestCommitHash(repository);
            }

            Commit commit = extractCommitInfo(commitHash, repository);

            if (repositoryTypeOrUserName.equals(RepositoryType.TESTS.getName())) {
                processNewPushToTestsRepository(exercise, commitHash, (SolutionProgrammingExerciseParticipation) participation);
                return;
            }

            // Process push to any repository other than the tests repository.
            processNewPushToRepository(participation, commit);

        }
        catch (GitAPIException | IOException e) {
            // This catch clause does not catch exceptions that happen during runBuildJob() as that method is called asynchronously.
            // For exceptions happening inside runBuildJob(), the user is notified. See the addBuildJobToQueue() method in the LocalCIBuildJobExecutionService for that.
            throw new LocalCIException("Could not process new push to repository " + localVCRepositoryUrl.getURI() + ". No build job was queued.", e);
        }

        log.info("New push processed to repository {} in {}. A build job was queued.", localVCRepositoryUrl.getURI(), TimeLogUtil.formatDurationFrom(timeNanoStart));
    }

    private LocalVCRepositoryUrl getLocalVCRepositoryUrl(Path repositoryFolderPath) {
        try {
            return new LocalVCRepositoryUrl(repositoryFolderPath, localVCBaseUrl);
        }
        catch (LocalVCInternalException e) {
            // This means something is misconfigured.
            throw new LocalCIException("Could not create valid repository URL from path " + repositoryFolderPath, e);
        }
    }

    private String getLatestCommitHash(Repository repository) throws GitAPIException {
        try (Git git = new Git(repository)) {
            RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();
            return latestCommit.getName();
        }
    }

    /**
     * Process a new push to the tests repository.
     * Build and test the solution repository to make sure all tests are still passing.
     *
     * @param exercise   the exercise for which the push was made.
     * @param commitHash the hash of the last commit to the tests repository.
     * @throws LocalCIException if something unexpected goes wrong creating the submission or triggering the build.
     */
    private void processNewPushToTestsRepository(ProgrammingExercise exercise, String commitHash, SolutionProgrammingExerciseParticipation solutionParticipation) {
        // Create a new submission for the solution repository.
        ProgrammingSubmission submission;
        try {
            submission = programmingSubmissionService.createSolutionParticipationSubmissionWithTypeTest(exercise.getId(), commitHash);
        }
        catch (EntityNotFoundException | IllegalStateException e) {
            throw new LocalCIException("Could not create submission for solution participation", e);
        }

        programmingMessagingService.notifyUserAboutSubmission(submission);

        try {
            // Set a flag to inform the instructor that the student results are now outdated.
            programmingTriggerService.setTestCasesChanged(exercise.getId(), true);
        }
        catch (EntityNotFoundException e) {
            throw new LocalCIException("Could not set test cases changed flag", e);
        }

        // Trigger a build of the solution repository.
        CompletableFuture<LocalCIBuildResult> futureSolutionBuildResult = localCIBuildJobExecutionService.addBuildJobToQueue(solutionParticipation);
        futureSolutionBuildResult.thenAccept(buildResult -> {

            // The 'user' is not properly logged into Artemis, this leads to an issue when accessing custom repository methods.
            // Therefore, a mock auth object has to be created.
            SecurityUtils.setAuthorizationObject();
            Result result = programmingExerciseGradingService.processNewProgrammingExerciseResult(solutionParticipation, buildResult).orElseThrow();
            programmingMessagingService.notifyUserAboutNewResult(result, solutionParticipation);

            // The solution participation received a new result, also trigger a build of the template repository.
            try {
                programmingTriggerService.triggerTemplateBuildAndNotifyUser(exercise.getId(), submission.getCommitHash(), SubmissionType.TEST);
            }
            catch (EntityNotFoundException | LocalCIException e) {
                // Something went wrong while retrieving the template participation or triggering the template build.
                // At this point, programmingMessagingService.notifyUserAboutSubmissionError() does not work, because the template participation is not available.
                // The instructor will see in the UI that no build of the template repository was conducted and will receive an error message when triggering the build manually.
                log.error("Something went wrong while triggering the template build for exercise " + exercise.getId() + " after the solution build was finished.", e);
            }
        });
    }

    /**
     * Process a new push to a student's repository or to the template or solution repository of the exercise.
     */
    private void processNewPushToRepository(ProgrammingExerciseParticipation participation, Commit commit) {
        // The 'user' is not properly logged into Artemis, this leads to an issue when accessing custom repository methods.
        // Therefore, a mock auth object has to be created.
        SecurityUtils.setAuthorizationObject();
        ProgrammingSubmission submission;
        try {
            submission = programmingSubmissionService.processNewProgrammingSubmission(participation, commit);
        }
        catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            throw new LocalCIException("Could not process submission for participation", e);
        }

        // Remove unnecessary information from the new submission.
        submission.getParticipation().setSubmissions(null);
        programmingMessagingService.notifyUserAboutSubmission(submission);

        // Trigger the build for the new submission on the local CI system.
        localCITriggerService.triggerBuild(participation);
    }

    private Commit extractCommitInfo(String commitHash, Repository repository) throws IOException, GitAPIException {
        RevCommit revCommit;
        String branch = null;

        ObjectId objectId = repository.resolve(commitHash);

        if (objectId == null) {
            throw new LocalCIException("Could not resolve commit hash " + commitHash + " in repository");
        }

        revCommit = repository.parseCommit(objectId);

        // Get the branch name.
        Git git = new Git(repository);
        // Look in the 'refs/heads' namespace for a ref that points to the commit.
        // The returned map contains at most one entry where the key is the commit id and the value denotes the branch which points to it.
        Map<ObjectId, String> objectIdBranchNameMap = git.nameRev().addPrefix("refs/heads").add(objectId).call();
        if (!objectIdBranchNameMap.isEmpty()) {
            branch = objectIdBranchNameMap.get(objectId);
        }
        git.close();

        if (revCommit == null || branch == null) {
            throw new LocalCIException("Something went wrong retrieving the revCommit or the branch.");
        }

        Commit commit = new Commit();
        commit.setCommitHash(commitHash);
        commit.setAuthorName(revCommit.getAuthorIdent().getName());
        commit.setAuthorEmail(revCommit.getAuthorIdent().getEmailAddress());
        commit.setBranch(branch);
        commit.setMessage(revCommit.getFullMessage());

        return commit;
    }

    /**
     * Get the participation for a given exercise and a repository type or user name. This method is used by the local VC system and by the local CI system to get the
     * participation.
     *
     * @param exercise                 the exercise for which to get the participation.
     * @param repositoryTypeOrUserName the repository type ("exercise", "solution", or "tests") or username (e.g. "artemis_test_user_1") as extracted from the repository URL.
     * @param isPracticeRepository     whether the repository is a practice repository, i.e. the repository URL contains "-practice-".
     * @param withSubmissions          whether submissions should be loaded with the participation. This is needed for the local CI system.
     * @return the participation.
     * @throws EntityNotFoundException if the participation could not be found.
     */
    public ProgrammingExerciseParticipation getParticipationForRepository(ProgrammingExercise exercise, String repositoryTypeOrUserName, boolean isPracticeRepository,
            boolean withSubmissions) {

        // For pushes to the tests repository, the solution repository is built first, and thus we need the solution participation.
        if (repositoryTypeOrUserName.equals(RepositoryType.SOLUTION.toString()) || repositoryTypeOrUserName.equals(RepositoryType.TESTS.toString())) {
            if (withSubmissions) {
                return solutionParticipationRepository.findWithEagerResultsAndSubmissionsByProgrammingExerciseIdElseThrow(exercise.getId());
            }
            else {
                return solutionParticipationRepository.findByProgrammingExerciseIdElseThrow(exercise.getId());
            }
        }

        if (repositoryTypeOrUserName.equals(RepositoryType.TEMPLATE.toString())) {
            if (withSubmissions) {
                return templateParticipationRepository.findWithEagerResultsAndSubmissionsByProgrammingExerciseIdElseThrow(exercise.getId());
            }
            else {
                return templateParticipationRepository.findByProgrammingExerciseIdElseThrow(exercise.getId());
            }
        }

        if (exercise.isTeamMode()) {
            // The repositoryTypeOrUserName is the team short name.
            // For teams, there is no practice participation.
            return programmingExerciseParticipationService.findTeamParticipationByExerciseAndTeamShortNameOrThrow(exercise, repositoryTypeOrUserName, withSubmissions);
        }

        // If the exercise is an exam exercise and the repository's owner is at least an editor, the repository could be a test run repository, or it could be the instructor's
        // assignment repository.
        // There is no way to tell from the repository URL, and only one participation will be created, even if both are used.
        // This participation has "testRun = true" set if the test run was created first, and "testRun = false" set if the instructor's assignment repository was created first.
        // If the exercise is an exam exercise, and the repository's owner is at least an editor, get the participation without regard for the testRun flag.
        boolean isExamEditorRepository = exercise.isExamExercise()
                && authorizationCheckService.isAtLeastEditorForExercise(exercise, userRepository.getUserByLoginElseThrow(repositoryTypeOrUserName));
        if (isExamEditorRepository) {
            if (withSubmissions) {
                return studentParticipationRepository.findWithSubmissionsByExerciseIdAndStudentLoginOrThrow(exercise.getId(), repositoryTypeOrUserName);
            }

            return studentParticipationRepository.findByExerciseIdAndStudentLoginOrThrow(exercise.getId(), repositoryTypeOrUserName);
        }

        return programmingExerciseParticipationService.findStudentParticipationByExerciseAndStudentLoginAndTestRunOrThrow(exercise, repositoryTypeOrUserName, isPracticeRepository,
                withSubmissions);
    }
}
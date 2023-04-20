package de.tum.in.www1.artemis.service.connectors.localci;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.BadRequestException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;

import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.exception.LocalCIException;
import de.tum.in.www1.artemis.service.connectors.ci.ContinuousIntegrationService;
import de.tum.in.www1.artemis.service.connectors.localci.dto.LocalCIBuildResult;
import de.tum.in.www1.artemis.service.util.TimeLogUtil;

/**
 * Service for running build jobs on the local CI server.
 */
@Service
@Profile("localci")
public class LocalCIBuildJobService {

    private final Logger log = LoggerFactory.getLogger(LocalCIBuildJobService.class);

    private final LocalCIBuildPlanService localCIBuildPlanService;

    private final DockerClient dockerClient;

    @Value("${artemis.continuous-integration.build.images.java.default}")
    String dockerImage;

    public LocalCIBuildJobService(LocalCIBuildPlanService localCIBuildPlanService, DockerClient dockerClient) {
        this.localCIBuildPlanService = localCIBuildPlanService;
        this.dockerClient = dockerClient;
    }

    /**
     * @param assignmentRepositoryPath The path to the assignment repository.
     * @param testsRepositoryPath      The path to the test repository.
     * @param scriptPath               The path to the shell script that should be executed in the container.
     * @param branch                   The branch that should be built.
     * @return the container id of the created container.
     * @throws LocalCIException if the container could not be created.
     */
    public String prepareDockerContainer(Path assignmentRepositoryPath, Path testsRepositoryPath, Path scriptPath, String branch) {
        HostConfig volumeConfig = createVolumeConfig(assignmentRepositoryPath, testsRepositoryPath, scriptPath);

        // If the docker image is not available on the local machine, pull it from Docker Hub.
        pullDockerImage(dockerClient, dockerImage);

        // Create the container from the "ls1tum/artemis-maven-template" image with the local paths to the Git repositories and the shell script bound to it.
        CreateContainerResponse container = createContainer(volumeConfig, branch);

        return container.getId();
    }

    /**
     * Runs the build job. This includes creating and starting a Docker container, executing the build script, and processing the build result.
     *
     * @param participation The participation for which the build job should be run.
     * @param containerId   The id of the container that should be used for the build job.
     * @param branch        The branch that should be built.
     * @param scriptPath    The path to the build script.
     * @return The build result.
     * @throws LocalCIException if something went wrong while running the build job.
     */
    public LocalCIBuildResult runBuildJob(ProgrammingExerciseParticipation participation, String containerId, String branch, Path scriptPath) {

        long timeNanoStart = System.nanoTime();

        dockerClient.startContainerCmd(containerId).exec();

        runScriptInContainer(containerId);

        ZonedDateTime buildCompletedDate = ZonedDateTime.now();

        String assignmentRepoCommitHash = "";
        String testsRepoCommitHash = "";

        try {
            assignmentRepoCommitHash = getCommitHashOfBranch(containerId, "assignment-repository", branch);
            testsRepoCommitHash = getCommitHashOfBranch(containerId, "test-repository", branch);
        }
        catch (NotFoundException | IOException e) {
            // Could not read commit hash from .git folder. Stop the container and return a build result that indicates that the build failed (empty list for failed tests and
            // empty list for successful tests).
            stopContainer(containerId);
            deleteTemporaryBuildScript(scriptPath);
            return constructFailedBuildResult(branch, assignmentRepoCommitHash, testsRepoCommitHash, buildCompletedDate);
        }

        // When Gradle is used as the build tool, the test results are located in /repositories/test-repository/build/test-results/test/TEST-*.xml.
        // When Maven is used as the build tool, the test results are located in /repositories/test-repository/target/surefire-reports/TEST-*.xml.
        String testResultsPath = "/repositories/test-repository/build/test-results/test";

        // Get an input stream of the test result files.
        TarArchiveInputStream testResultsTarInputStream;
        try {
            testResultsTarInputStream = new TarArchiveInputStream(dockerClient.copyArchiveFromContainerCmd(containerId, testResultsPath).exec());
        }
        catch (NotFoundException e) {
            // If the test results are not found, this means that something went wrong during the build and testing of the submission.
            // Stop the container and return a build results that indicates that the build failed.
            stopContainer(containerId);
            deleteTemporaryBuildScript(scriptPath);
            return constructFailedBuildResult(branch, assignmentRepoCommitHash, testsRepoCommitHash, buildCompletedDate);
        }

        stopContainer(containerId);
        deleteTemporaryBuildScript(scriptPath);

        LocalCIBuildResult buildResult;
        try {
            buildResult = parseTestResults(testResultsTarInputStream, branch, assignmentRepoCommitHash, testsRepoCommitHash, buildCompletedDate);
        }
        catch (IOException | XMLStreamException | IllegalStateException e) {
            throw new LocalCIException("Error while parsing test results", e);
        }

        // Set the build status to "INACTIVE" to indicate that the build is not running anymore.
        localCIBuildPlanService.updateBuildPlanStatus(participation, ContinuousIntegrationService.BuildStatus.INACTIVE);

        log.info("Building and testing submission for repository {} took {}", participation.getRepositoryUrl(), TimeLogUtil.formatDurationFrom(timeNanoStart));

        return buildResult;
    }

    private String getCommitHashOfBranch(String containerId, String repositoryName, String branchName) throws IOException {
        // Get an input stream of the file in .git folder of the repository that contains the current commit hash of the branch.
        TarArchiveInputStream repositoryTarInputStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(containerId, "/repositories/" + repositoryName + "/.git/refs/heads/" + branchName).exec());
        repositoryTarInputStream.getNextTarEntry();
        String commitHash = IOUtils.toString(repositoryTarInputStream, StandardCharsets.UTF_8).replace("\n", "");
        repositoryTarInputStream.close();
        return commitHash;
    }

    private void runScriptInContainer(String containerId) {
        // The "sh script.sh" command specified here is run inside the container as an additional process. This command runs in the background, independent of the container's
        // main process. The execution command can run concurrently with the main process. This setup with the ExecCreateCmdResponse gives us the ability to wait in code until the
        // command has finished before trying to extract the results.
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId).withAttachStdout(true).withAttachStderr(true).withCmd("sh", "script.sh").exec();

        // Start the command and wait for it to complete.
        final CountDownLatch latch = new CountDownLatch(1);
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ResultCallback.Adapter<>() {

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            // Block until the latch reaches 0 or until the thread is interrupted.
            latch.await();
        }
        catch (InterruptedException e) {
            throw new LocalCIException("Interrupted while waiting for command to complete", e);
        }
    }

    private CreateContainerResponse createContainer(HostConfig volumeConfig, String branch) {
        return dockerClient.createContainerCmd(dockerImage).withHostConfig(volumeConfig).withEnv("ARTEMIS_BUILD_TOOL=gradle", "ARTEMIS_DEFAULT_BRANCH=" + branch)
                // Command to run when the container starts. This is the command that will be executed in the container's main process, which runs in the foreground and blocks the
                // container from exiting until it finishes.
                // It waits until the script that is running the tests (see below execCreateCmdResponse) is completed, and until the result files are extracted which is indicated
                // by the creation of a file "stop_container.txt" in the container's root directory.
                .withCmd("sh", "-c", "while [ ! -f /stop_container.txt ]; do sleep 0.5; done")
                // .withCmd("tail", "-f", "/dev/null") // Activate for debugging purposes instead of the above command to get a running container that you can peek into using
                // "docker exec -it <container-id> /bin/bash".
                .exec();
    }

    private HostConfig createVolumeConfig(Path assignmentRepositoryPath, Path testRepositoryPath, Path scriptPath) {
        // Configure the volumes of the container such that it can access the assignment repository, the test repository, and the build script.
        return HostConfig.newHostConfig().withAutoRemove(true) // Automatically remove the container when it exits.
                .withBinds(new Bind(assignmentRepositoryPath.toString(), new Volume("/assignment-repository")),
                        new Bind(testRepositoryPath.toString(), new Volume("/test-repository")), new Bind(scriptPath.toString(), new Volume("/script.sh")));
    }

    private void pullDockerImage(DockerClient dockerClient, String dockerImage) {
        try {
            dockerClient.inspectImageCmd(dockerImage).exec();
        }
        catch (NotFoundException e) {
            // Image does not exist locally, pull it from Docker Hub.
            log.info("Pulling docker image {}", dockerImage);
            try {
                dockerClient.pullImageCmd(dockerImage).exec(new PullImageResultCallback()).awaitCompletion();
            }
            catch (InterruptedException ie) {
                throw new LocalCIException("Interrupted while pulling docker image " + dockerImage, ie);
            }
        }
        catch (BadRequestException e) {
            throw new LocalCIException("Error while inspecting docker image " + dockerImage, e);
        }
    }

    /**
     * Stops the container with the given id by creating a file "stop_container.txt" in its root directory.
     * The container was created in such a way that it waits for this file to appear and then stops running, causing it to be removed at the same time.
     *
     * @param containerId The id of the container to stop.
     */
    public void stopContainer(String containerId) {
        if (!isContainerRunning(containerId)) {
            return;
        }
        // Create a file "stop_container.txt" in the root directory of the container to indicate that the test results have been extracted or that the container should be stopped
        // for some other reason.
        // The container's main process is waiting for this file to appear and then stops the main process, thus stopping and removing the container.
        ExecCreateCmdResponse createStopContainerFileCmdResponse = dockerClient.execCreateCmd(containerId).withCmd("touch", "stop_container.txt").exec();
        dockerClient.execStartCmd(createStopContainerFileCmdResponse.getId()).exec(new ResultCallback.Adapter<>());
    }

    private boolean isContainerRunning(String containerId) {
        // List all containers, including the non-running ones.
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

        // Check if there's a container with the given ID and if it's running.
        return containers.stream().filter(container -> container.getId().equals(containerId)).findFirst().map(container -> container.getState().equals("running")).orElse(false);
    }

    /**
     * Deletes the build script if it was created as a temporary file.
     *
     * @param scriptPath The path of the build script.
     */
    public void deleteTemporaryBuildScript(Path scriptPath) {
        // If the script was created as a temporary file, delete it.
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        if (scriptPath.startsWith(tempDir)) {
            try {
                Files.deleteIfExists(scriptPath);
            }
            catch (IOException e) {
                log.error("Could not delete temporary file {}", scriptPath);
            }
        }
    }

    private LocalCIBuildResult parseTestResults(TarArchiveInputStream testResultsTarInputStream, String assignmentRepoBranchName, String assignmentRepoCommitHash,
            String testsRepoCommitHash, ZonedDateTime buildCompletedDate) throws IOException, XMLStreamException {

        boolean isBuildSuccessful = true;

        List<LocalCIBuildResult.LocalCITestJobDTO> failedTests = new ArrayList<>();
        List<LocalCIBuildResult.LocalCITestJobDTO> successfulTests = new ArrayList<>();

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        TarArchiveEntry tarEntry;
        while ((tarEntry = testResultsTarInputStream.getNextTarEntry()) != null) {

            if (tarEntry.isDirectory() || !tarEntry.getName().endsWith(".xml") || !tarEntry.getName().startsWith("test/TEST-")) {
                continue;
            }

            // Read the contents of the tar entry as a string.
            String xmlString = IOUtils.toString(testResultsTarInputStream, StandardCharsets.UTF_8);

            // Create an XML stream reader for the string.
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StringReader(xmlString));

            // Move to the first start element.
            while (xmlStreamReader.hasNext() && !xmlStreamReader.isStartElement()) {
                xmlStreamReader.next();
            }

            // Check if the start element is the "testsuite" node.
            if (!("testsuite".equals(xmlStreamReader.getLocalName()))) {
                throw new IllegalStateException("Expected testsuite element, but got " + xmlStreamReader.getLocalName());
            }

            // Go through all testcase nodes.
            while (xmlStreamReader.hasNext()) {
                xmlStreamReader.next();

                if (!xmlStreamReader.isStartElement() || !("testcase".equals(xmlStreamReader.getLocalName()))) {
                    continue;
                }

                // Now we are at the start of a "testcase" node.

                // Extract the name attribute from the "testcase" node.
                String name = xmlStreamReader.getAttributeValue(null, "name");

                // Check if there is a failure node inside the testcase node.
                // Call next() until there is an end element (no failure node exists inside the testcase node) or a start element (failure node exists inside the
                // testcase node).
                xmlStreamReader.next();
                while (!(xmlStreamReader.isEndElement() || xmlStreamReader.isStartElement())) {
                    xmlStreamReader.next();
                }
                if (xmlStreamReader.isStartElement() && "failure".equals(xmlStreamReader.getLocalName())) {
                    // Extract the message attribute from the "failure" node.
                    String error = xmlStreamReader.getAttributeValue(null, "message");

                    // Add the failed test to the list of failed tests.
                    List<String> errors = error != null ? List.of(error) : List.of();
                    failedTests.add(new LocalCIBuildResult.LocalCITestJobDTO(name, errors));

                    // If there is at least one test case with a failure node, the build is not successful.
                    isBuildSuccessful = false;
                }
                else {
                    // Add the successful test to the list of successful tests.
                    successfulTests.add(new LocalCIBuildResult.LocalCITestJobDTO(name, List.of()));
                }
            }
            // Close the XML stream reader.
            xmlStreamReader.close();
        }

        return constructBuildResult(failedTests, successfulTests, assignmentRepoBranchName, assignmentRepoCommitHash, testsRepoCommitHash, isBuildSuccessful, buildCompletedDate);
    }

    private LocalCIBuildResult constructFailedBuildResult(String assignmentRepoBranchName, String assignmentRepoCommitHash, String testsRepoCommitHash,
            ZonedDateTime buildRunDate) {
        return constructBuildResult(List.of(), List.of(), assignmentRepoBranchName, assignmentRepoCommitHash, testsRepoCommitHash, false, buildRunDate);
    }

    private LocalCIBuildResult constructBuildResult(List<LocalCIBuildResult.LocalCITestJobDTO> failedTests, List<LocalCIBuildResult.LocalCITestJobDTO> successfulTests,
            String assignmentRepoBranchName, String assignmentRepoCommitHash, String testsRepoCommitHash, boolean isBuildSuccessful, ZonedDateTime buildRunDate) {
        LocalCIBuildResult.LocalCIJobDTO job = new LocalCIBuildResult.LocalCIJobDTO(failedTests, successfulTests);

        return new LocalCIBuildResult(assignmentRepoBranchName, assignmentRepoCommitHash, testsRepoCommitHash, isBuildSuccessful, buildRunDate, List.of(job));
    }
}

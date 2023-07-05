package de.tum.in.www1.artemis.service.dataexport;

import static de.tum.in.www1.artemis.service.dataexport.DataExportExerciseCreationService.CSV_FILE_EXTENSION;
import static de.tum.in.www1.artemis.service.dataexport.DataExportUtil.retrieveCourseDirPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.DataExportState;
import de.tum.in.www1.artemis.exception.ArtemisMailException;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.notifications.MailService;
import de.tum.in.www1.artemis.service.notifications.SingleUserNotificationService;
import de.tum.in.www1.artemis.service.user.UserService;

/**
 * A service to create data exports for users
 */
@Service
public class DataExportCreationService {

    private static final String ZIP_FILE_EXTENSION = ".zip";

    private final Logger log = LoggerFactory.getLogger(DataExportCreationService.class);

    @Value("${artemis.data-export-path:./data-exports}")
    private Path dataExportsPath;

    private final ZipFileService zipFileService;

    private final ExerciseRepository exerciseRepository;

    private final FileService fileService;

    private final SingleUserNotificationService singleUserNotificationService;

    private final DataExportRepository dataExportRepository;

    private final MailService mailService;

    private final UserService userService;

    private final DataExportExerciseCreationService dataExportExerciseCreationService;

    private final DataExportExamCreationService dataExportExamCreationService;

    private final DataExportCommunicationDataService dataExportCommunicationDataService;

    public DataExportCreationService(ZipFileService zipFileService, ExerciseRepository exerciseRepository, FileService fileService,
            SingleUserNotificationService singleUserNotificationService, DataExportRepository dataExportRepository, MailService mailService, UserService userService,
            DataExportExerciseCreationService dataExportExerciseCreationService, DataExportExamCreationService dataExportExamCreationService,
            DataExportCommunicationDataService dataExportCommunicationDataService) {
        this.zipFileService = zipFileService;
        this.exerciseRepository = exerciseRepository;
        this.fileService = fileService;
        this.singleUserNotificationService = singleUserNotificationService;
        this.dataExportRepository = dataExportRepository;
        this.mailService = mailService;
        this.userService = userService;
        this.dataExportExerciseCreationService = dataExportExerciseCreationService;
        this.dataExportExamCreationService = dataExportExamCreationService;
        this.dataExportCommunicationDataService = dataExportCommunicationDataService;
    }

    /**
     * Creates the data export for the given user.
     * Retrieves all courses and exercises the user has participated in from the database.
     *
     * @param dataExport the data export to be created
     **/
    private DataExport createDataExportWithContent(DataExport dataExport) throws IOException {
        log.info("Creating data export for user {}", dataExport.getUser().getLogin());
        var userId = dataExport.getUser().getId();
        var user = dataExport.getUser();
        var workingDirectory = prepareDataExport(dataExport);
        createExercisesExport(workingDirectory, userId);
        dataExportExamCreationService.createExportForExams(userId, workingDirectory);
        dataExportCommunicationDataService.createCommunicationDataExport(userId, workingDirectory);
        addGeneralUserInformation(user, workingDirectory);
        var dataExportPath = createDataExportZipFile(user.getLogin(), workingDirectory);
        fileService.scheduleForDirectoryDeletion(workingDirectory, 30);
        return finishDataExportCreation(dataExport, dataExportPath);
    }

    private void createExercisesExport(Path workingDirectory, long userId) throws IOException {
        // retrieve all exercises as we cannot retrieve the exercises by course because a user might have participated in a course they are no longer a member of (they have
        // unenrolled)
        var allExerciseParticipations = exerciseRepository.getAllExercisesUserParticipatedInWithEagerParticipationsSubmissionsResultsFeedbacksByUserId(userId);
        var exerciseParticipationsPerCourse = allExerciseParticipations.stream().collect(Collectors.groupingBy(Exercise::getCourseViaExerciseGroupOrCourseMember));
        for (var entry : exerciseParticipationsPerCourse.entrySet()) {
            var course = entry.getKey();
            Path courseDir = retrieveCourseDirPath(workingDirectory, course);
            var exercises = entry.getValue();
            if (!exercises.isEmpty()) {
                Files.createDirectory(courseDir);
            }
            for (var exercise : exercises) {
                if (exercise instanceof ProgrammingExercise programmingExercise) {
                    dataExportExerciseCreationService.createProgrammingExerciseExport(programmingExercise, courseDir, userId);
                }
                else {
                    dataExportExerciseCreationService.createNonProgrammingExerciseExport(exercise, courseDir, userId);
                }
            }
        }
    }

    /**
     * Creates the data export for the given user.
     *
     * @param dataExport the data export to be created
     * @return true if the export was successful, false otherwise
     */
    public boolean createDataExport(DataExport dataExport) {
        try {
            dataExport = createDataExportWithContent(dataExport);
        }
        catch (Exception e) {
            log.error("Error while creating data export for user {}", dataExport.getUser().getLogin(), e);
            handleCreationFailure(dataExport, e);
            return false;
        }
        // the data export should be marked as successful even if the email cannot be sent.
        // The user still receives a webapp notification, that's why we wrap the following block in a try-catch
        try {
            singleUserNotificationService.notifyUserAboutDataExportCreation(dataExport);
        }
        catch (ArtemisMailException e) {
            log.warn("Failed to send email about successful data export creation");
        }
        return true;
    }

    private void handleCreationFailure(DataExport dataExport, Exception exception) {
        dataExport.setDataExportState(DataExportState.FAILED);
        dataExport = dataExportRepository.save(dataExport);
        singleUserNotificationService.notifyUserAboutDataExportFailure(dataExport);
        Optional<User> admin = userService.findInternalAdminUser();
        if (admin.isEmpty()) {
            log.warn("No internal admin user found. Cannot send email to admin about data export failure.");
            return;
        }
        mailService.sendDataExportFailedEmailToAdmin(admin.get(), dataExport, exception);
    }

    private DataExport finishDataExportCreation(DataExport dataExport, Path dataExportPath) {
        dataExport.setFilePath(dataExportPath.toString());
        dataExport.setCreationFinishedDate(ZonedDateTime.now());
        dataExport = dataExportRepository.save(dataExport);
        dataExport.setDataExportState(DataExportState.EMAIL_SENT);
        return dataExportRepository.save(dataExport);
    }

    private Path prepareDataExport(DataExport dataExport) throws IOException {
        if (!Files.exists(dataExportsPath)) {
            Files.createDirectories(dataExportsPath);
        }
        dataExport = dataExportRepository.save(dataExport);
        Path workingDirectory = Files.createTempDirectory(dataExportsPath, "data-export-working-dir");
        dataExport.setDataExportState(DataExportState.IN_CREATION);
        dataExportRepository.save(dataExport);
        return workingDirectory;
    }

    private void addGeneralUserInformation(User user, Path workingDirectory) throws IOException {
        String[] headers = { "login", "name", "email", "registration number" };
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(headers).build();

        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(workingDirectory.resolve("general_user_information" + CSV_FILE_EXTENSION)), csvFormat)) {
            printer.printRecord(user.getLogin(), user.getName(), user.getEmail(), user.getRegistrationNumber());
            printer.flush();
        }
    }

    private Path createDataExportZipFile(String userLogin, Path workingDirectory) throws IOException {
        // There should actually never exist more than one data export for a user at a time (once the feature is fully implemented), but to be sure the name is unique, we add the
        // current timestamp
        return zipFileService.createZipFileWithFolderContent(dataExportsPath.resolve("data-export_" + userLogin + ZonedDateTime.now().toEpochSecond() + ZIP_FILE_EXTENSION),
                workingDirectory, null);

    }
}
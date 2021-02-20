package de.tum.in.www1.artemis.service.connectors.jenkins;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.exception.ContinuousIntegrationException;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.connectors.CIUserManagementService;
import de.tum.in.www1.artemis.service.connectors.jenkins.dto.JenkinsUpdateUserDTO;
import de.tum.in.www1.artemis.service.connectors.jenkins.dto.JenkinsUserDTO;
import de.tum.in.www1.artemis.service.connectors.jenkins.jobs.JenkinsJobPermission;
import de.tum.in.www1.artemis.service.connectors.jenkins.jobs.JenkinsJobPermissionsService;
import de.tum.in.www1.artemis.service.user.PasswordService;

@Service
@Profile("jenkins")
public class JenkinsUserManagementService implements CIUserManagementService {

    private final Logger log = LoggerFactory.getLogger(JenkinsUserManagementService.class);

    @Value("${artemis.continuous-integration.url}")
    private URL jenkinsServerUrl;

    private final RestTemplate restTemplate;

    private final JenkinsJobPermissionsService jenkinsJobPermissionsService;

    protected final PasswordService passwordService;

    private final ProgrammingExerciseRepository programmingExerciseRepository;

    private final UserRepository userRepository;

    public JenkinsUserManagementService(@Qualifier("jenkinsRestTemplate") RestTemplate restTemplate, JenkinsJobPermissionsService jenkinsJobPermissionsService,
            PasswordService passwordService, ProgrammingExerciseRepository programmingExerciseRepository, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.jenkinsJobPermissionsService = jenkinsJobPermissionsService;
        this.passwordService = passwordService;
        this.programmingExerciseRepository = programmingExerciseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a user in Jenkins. Note that the user login acts as
     * a unique identifier in Jenkins.
     *
     * @param user The user to create
     */
    @Override
    public void createUser(User user) throws ContinuousIntegrationException {
        // Only create a user if it doesn't already exist.
        if (getUser(user) != null) {
            throw new JenkinsException("Cannot create user: " + user.getLogin() + " because the login already exists");
        }

        // Make sure the user login contains legal characters.
        if (!isUserLoginLegal(user)) {
            throw new JenkinsException("Cannot create user: " + user.getLogin() + " because the login contains illegal characters");
        }

        try {
            // Create the Jenkins user
            var uri = UriComponentsBuilder.fromHttpUrl(jenkinsServerUrl.toString()).pathSegment("securityRealm", "createAccountByAdmin").build().toUri();
            restTemplate.exchange(uri, HttpMethod.POST, getCreateUserFormHttpEntity(user), Void.class);

            // Adds the user to groups of existing programming exercises
            addUserToGroups(user, user.getGroups());
        }
        catch (RestClientException e) {
            throw new JenkinsException("Cannot create user: " + user.getLogin(), e);
        }
    }

    /**
     * Creates an HttpEntity containing the form data required by the POST request for creating a
     * new Jenkins user.
     *
     * @param user the user to create
     * @return http entity with the user encoded as the form data.
     */
    private HttpEntity<MultiValueMap<String, String>> getCreateUserFormHttpEntity(User user) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("username", user.getLogin());
        formData.add("password1", passwordService.decryptPassword(user));
        formData.add("password2", passwordService.decryptPassword(user));
        formData.add("fullname", user.getName());
        formData.add("email", user.getEmail());

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(formData, headers);
    }

    @Override
    public void deleteUser(User user) throws ContinuousIntegrationException {
        // Only delete a user if it exists.
        var jenkinsUser = getUser(user);
        if (jenkinsUser == null) {
            return;
        }

        // Make sure that the Jenkins user is the Artemis user by checking
        // if the emails are the same
        if (!emailEqualToJenkinsUser(user.getEmail(), jenkinsUser)) {
            return;
        }

        try {
            var uri = UriComponentsBuilder.fromHttpUrl(jenkinsServerUrl.toString()).pathSegment("user", user.getLogin(), "doDelete").build().toUri();
            restTemplate.exchange(uri, HttpMethod.POST, null, Void.class);
        }
        catch (RestClientException e) {
            throw new JenkinsException("Cannot delete user: " + user.getLogin(), e);
        }
    }

    /**
     * Updates the user in Jenkins with the user data from Artemis.
     * <p>
     * Note that it's not possible to change the username of the Jenkins user.
     *
     * @param user The user to update.
     */
    @Override
    public void updateUser(User user) throws ContinuousIntegrationException {
        // Only update a user if it exists.
        var jenkinsUser = getUser(user);
        if (jenkinsUser == null) {
            throw new JenkinsException("Cannot update user: " + user.getLogin() + " because it doesn't exist.");
        }

        // Make sure that the Jenkins user is the Artemis user by checking
        // if the emails are the same
        if (!emailEqualToJenkinsUser(user.getEmail(), jenkinsUser)) {
            return;
        }

        try {
            var uri = UriComponentsBuilder.fromHttpUrl(jenkinsServerUrl.toString()).pathSegment("user", user.getLogin(), "configSubmit").build().toUri();
            restTemplate.exchange(uri, HttpMethod.POST, getUpdateUserFormHttpEntity(user), String.class);
        }
        catch (RestClientException | JsonProcessingException e) {
            throw new JenkinsException("Cannot update user: " + user.getLogin(), e);
        }
    }

    @Override
    public void updateUserAndGroups(User user, Set<String> groupsToAdd, Set<String> groupsToRemove) throws ContinuousIntegrationException {
        updateUser(user);
        addUserToGroups(user, groupsToAdd);
        removeUserFromGroups(user, groupsToRemove);
    }

    /**
     * Adds the Artemis user to a group in Jenkins. Jenkins does not support
     * groups so this function fetches all programming exercises belonging to
     * the groups and assigns the user permissions to them.
     *
     * @param user   The Artemis user to add to the group
     * @param groups The groups to add the user to
     */
    @Override
    public void addUserToGroups(User user, Set<String> groups) throws ContinuousIntegrationException {
        var exercises = programmingExerciseRepository.findAllByInstructorOrTAGroupNameIn(groups);
        exercises.forEach(exercise -> {
            // The exercise's project key is also the name of the Jenkins job that groups all build plans
            // for students, solution, and template.
            var jobName = exercise.getProjectKey();
            var userLogin = user.getLogin();
            var course = exercise.getCourseViaExerciseGroupOrCourseMember();

            if (groups.contains(course.getInstructorGroupName())) {
                try {
                    // We are assigning instructor permissions since the exercise's course teaching assistant group
                    // is the same as the one that is specified.
                    jenkinsJobPermissionsService.addPermissionsForUserToFolder(userLogin, jobName, JenkinsJobPermission.getInstructorPermissions());
                }
                catch (IOException e) {
                    throw new JenkinsException("Cannot assign instructor permissions to user: " + userLogin, e);
                }
            }
            else if (groups.contains(course.getTeachingAssistantGroupName())) {
                try {
                    // We are assigning teaching assistant permissions since the exercise's course teaching assistant group
                    // is the same as the one that is specified.
                    jenkinsJobPermissionsService.addTeachingAssistantPermissionsToUserForFolder(userLogin, jobName);
                }
                catch (IOException e) {
                    throw new JenkinsException("Cannot assign teaching assistant permissions to user: " + userLogin, e);
                }
            }

        });
    }

    /**
     * Removes the Artemis user from the specified groups. Jenkins doesn't support groups so this function fetches
     * all programming exercises belonging to the groups, and revokes the user's permissions from them.
     *
     * @param user   The Artemis user to remove from the group
     * @param groups The groups to remove the user from
     */
    @Override
    public void removeUserFromGroups(User user, Set<String> groups) throws ContinuousIntegrationException {
        var userLogin = user.getLogin();
        // Remove all permissions assigned to the user for each exercise that belongs to the specified groups.
        var exercises = programmingExerciseRepository.findAllByInstructorOrTAGroupNameIn(groups);
        exercises.forEach(exercise -> {
            try {
                // The exercise's projectkey is also the name of the Jenkins folder job which groups the student's, solution,
                // and template build plans together
                var jobName = exercise.getProjectKey();
                jenkinsJobPermissionsService.removePermissionsFromUserOfFolder(userLogin, jobName, Set.of(JenkinsJobPermission.values()));
            }
            catch (IOException e) {
                throw new JenkinsException("Cannot revoke permissions from user: " + userLogin, e);
            }
        });

        // The same user can belong to a TA and instructor group. Adding the user to an instructor group
        // automatically overwrites the TA permissions. If the user is removed from the instructor group,
        // we need to re-apply TA permissions.
        addUserToGroups(user, user.getGroups());
    }

    @Override
    public void updateCoursePermissions(Course updatedCourse, String oldInstructorGroup, String oldTeachingAssistantGroup) throws ContinuousIntegrationException {
        var newInstructorGroup = updatedCourse.getInstructorGroupName();
        var newTeachingAssistangGroup = updatedCourse.getTeachingAssistantGroupName();

        // Don't do anything if the groups didn't change
        if (newInstructorGroup.equals(oldInstructorGroup) && newTeachingAssistangGroup.equals(oldTeachingAssistantGroup)) {
            return;
        }

        // Remove all permissions assigned to the instructors and teaching assistants that do not belong to the course
        // anymore.
        removePermissionsFromInstructorsAndTAsForCourse(oldInstructorGroup, oldTeachingAssistantGroup, updatedCourse);

        // Assign teaching assistant and instructor permissions
        assignPermissionsToInstructorAndTAsForCourse(updatedCourse);
    }

    /**
     * Assigns teaching assistant and/or instructor permissions to each user belonging to the teaching assistant/instructor
     * groups of the course.
     *
     * @param course the course
     */
    private void assignPermissionsToInstructorAndTAsForCourse(Course course) {
        var teachingAssistants = userRepository.findAllInGroupWithAuthorities(course.getTeachingAssistantGroupName()).stream().map(User::getLogin).collect(Collectors.toSet());
        var instructors = userRepository.findAllInGroupWithAuthorities(course.getInstructorGroupName()).stream().map(User::getLogin).collect(Collectors.toSet());

        // Courses can have the same groups. We do not want to add/remove users from exercises of other courses
        // belonging to the same group
        var exercises = programmingExerciseRepository.findAllByCourse(course);
        exercises.forEach(exercise -> {
            var job = exercise.getProjectKey();
            try {
                jenkinsJobPermissionsService.addInstructorAndTAPermissionsToUsersForFolder(teachingAssistants, instructors, job);
            }
            catch (IOException e) {
                throw new JenkinsException("Cannot assign teaching assistant and instructor permissions for job: " + job, e);
            }
        });

    }

    /**
     * Removes all permissions assigned to instructors and teaching assistants for the specified course. The function
     * fetches all exercises that belong to the course and removes all permissions assigned to all instructors and
     * teaching assistants belonging to the groups.
     *
     * @param instructorGroup the group of instructors
     * @param teachingAssistantGroup the group of teaching assistants
     * @param course the course
     */
    private void removePermissionsFromInstructorsAndTAsForCourse(String instructorGroup, String teachingAssistantGroup, Course course) {
        // Courses can have the same groups. We do not want to add/remove users from exercises of other courses
        // belonging to the same group
        var exercises = programmingExerciseRepository.findAllByCourse(course);

        // Fetch all instructors and teaching assistants belonging to the group that was removed from the course.
        var oldInstructors = userRepository.findAllInGroupWithAuthorities(instructorGroup);
        var oldTeachingAssistants = userRepository.findAllInGroupWithAuthorities(teachingAssistantGroup);
        var usersFromOldGroup = Stream.concat(oldInstructors.stream(), oldTeachingAssistants.stream()).collect(Collectors.toList()).stream().map(User::getLogin)
                .collect(Collectors.toSet());

        // Revoke all permissions.
        exercises.forEach(exercise -> {
            try {
                jenkinsJobPermissionsService.removePermissionsFromUsersForFolder(usersFromOldGroup, exercise.getProjectKey(), Set.of(JenkinsJobPermission.values()));
            }
            catch (IOException e) {
                throw new JenkinsException("Cannot remove permissions from all users for job: " + exercise.getProjectKey(), e);
            }
        });
    }

    /**
     * Creates an HttpEntity containing the form data required by the POST request for updating an
     * existing Jenkins user.
     * <p>
     * Note: This will overwrite various fields like "description, primary view, ..."
     * <p>
     * TODO: https://stackoverflow.com/questions/17716242/creating-user-in-jenkins-via-api this might help to update users correctly.
     *
     * @param user The user to update
     * @return http entity with the user encoded as the form data
     * @throws JsonProcessingException if the user can't be parsed into json.
     */
    private HttpEntity<MultiValueMap<String, String>> getUpdateUserFormHttpEntity(User user) throws JsonProcessingException {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("user.password", passwordService.decryptPassword(user));
        formData.add("user.password2", passwordService.decryptPassword(user));
        formData.add("_.fullName", user.getName());
        formData.add("email.address", user.getEmail());
        formData.add("_.description", "");
        formData.add("_.primaryViewName", "");
        formData.add("providerId", "default");
        formData.add("_.authorizedKeys", "");
        formData.add("insensitiveSearch", "on");
        formData.add("_.timeZoneName", "");
        formData.add("core:apply", "true");
        formData.add("json", getUpdateUserJson(user));

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(formData, headers);
    }

    /**
     * Returns json containing information about the user to update in Jenkins.
     * This is required in addition to the form data.
     *
     * @param user The user to update
     * @return Json for Jenkins
     * @throws JsonProcessingException when something goes wrong writing the json content.
     */
    private String getUpdateUserJson(User user) throws JsonProcessingException {
        var updateUserDto = new JenkinsUpdateUserDTO();
        updateUserDto.setFullName(user.getName());
        updateUserDto.setDescription("");
        updateUserDto.setAddress(user.getEmail());
        updateUserDto.setPrimaryViewName("");
        updateUserDto.setProviderId("default");
        updateUserDto.setPassword(user.getPassword());
        updateUserDto.setAuthorizedKeys("");
        updateUserDto.setInsensitiveSearch(true);
        updateUserDto.setTimeZoneName("");
        return new ObjectMapper().writeValueAsString(updateUserDto);
    }

    /**
     * Gets a Jenkins user if it exists. Otherwise returns
     * null.
     *
     * @param user the Artemis look up
     * @return the Jenkins user or null if the user doesn't exist
     */
    private JenkinsUserDTO getUser(User user) throws ContinuousIntegrationException {
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(jenkinsServerUrl.toString()).pathSegment("user", user.getLogin(), "api", "json").build().toUri();
            return restTemplate.exchange(uri, HttpMethod.GET, null, JenkinsUserDTO.class).getBody();

        }
        catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return null;
            }

            var errorMessage = "Could not get user " + user.getLogin();
            log.error(errorMessage + ": " + e);
            throw new JenkinsException(errorMessage, e);
        }
    }

    /**
     * Returns true/false whether the given email is the same as the email of the Jenkins
     * user.
     * @param email to email
     * @param jenkinsUser the Jenkins user
     * @return true or false
     */
    private boolean emailEqualToJenkinsUser(String email, JenkinsUserDTO jenkinsUser) {
        var addressProperty = jenkinsUser.property.stream().filter(property -> property._class.equals("hudson.tasks.Mailer$UserProperty")).findFirst();
        return addressProperty.map(property -> property.address.equals(email)).orElse(false);
    }

    /**
     * The Jenkins username acts as a unique identifier and
     * can only contain alphanumeric characters, underscore and dash
     *
     * @param user The user
     * @return whether the user login is legal or not
     */
    private boolean isUserLoginLegal(User user) {
        String regex = "^[a-zA-Z0-9_-]*$";
        return user.getLogin().matches(regex);
    }
}

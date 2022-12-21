package de.tum.in.www1.artemis.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public abstract class VcsRepositoryUri {

    protected String username;

    protected String projectKey;

    protected String repositorySlug;

    protected VcsRepositoryUri() {
        // NOTE: this constructor should not be used and only exists to prevent compile errors
    }

    // Create the url from a uriSpecString, e.g. https://ab123cd@bitbucket.ase.in.tum.de/scm/EIST2016RME/RMEXERCISE-ab123cd
    public abstract VcsRepositoryUri getVcsRepositoryUriFromSpecString(String uriSpecString);

    // Create the uri from a file reference, e.g. C:/Users/Admin/AppData/Local/Temp/studentOriginRepo1644180397872264950
    public VcsRepositoryUri getVcsRepositoryUriFromFileReference(java.io.File file) {
        this.uri = file.toURI();
    }

    public VcsRepositoryUri withUser(final String username) {
        this.username = username;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // we explicitly allow subclasses (i.e. obj is a subclass of this) here (to avoid issues when comparing subclasses with the same url)
        // Note that this also includes the null check
        if (!(obj instanceof VcsRepositoryUri that)) {
            return false;
        }
        return Objects.equals(username, that.username) && Objects.equals(projectKey, that.projectKey) && Objects.equals(repositorySlug, that.repositorySlug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, projectKey, repositorySlug);
    }

    public abstract String getRepositoryUrl(String gitServerUrl);

    /**
     * Generates the unique local folder name for a given file or remote repository URI.
     * For file URIs, we take the last element of the path, which should be unique
     * For URLs pointing to remote git repositories, we use the whole path
     * <p>
     * Examples:
     * https://bitbucket.ase.in.tum.de/scm/eist20l06e03/eist20l06e03-ab123cd.git --> eist20l06e03/eist20l06e03-ab123cd
     * ssh://git@bitbucket.ase.in.tum.de:7999/eist20l06e03/eist20l06e03-ab123cd.git --> eist20l06e03/eist20l06e03-ab123cd
     * file:C:/Users/Admin/AppData/Local/Temp/studentOriginRepo1644180397872264950 --> studentOriginRepo1644180397872264950
     * file:/var/folders/vc/sk85td_s54v7w9tjq07b0_q80000gn/T/studentTeamOriginRepo420037178325056205/ -->  studentTeamOriginRepo420037178325056205
     * https://localhost:8080/git/EXAMPLECOURSE1/TESTEX1/examplecourse1testex1-exercise --> EXAMPLECOURSE1/TESTEX1/examplecourse1testex1-exercise
     *
     * @return the folderName as a string.
     */
    public String folderNameForRepositoryUrl() {
        if ("file".equals(uri.getScheme())) {
            // Take the last element of the path
            final var segments = uri.getPath().split("/");
            return segments[segments.length - 1];
        } else { // e.g. http(s) or ssh
            String path = getURI().getPath();
            path = path.replaceAll(".git$", "");
            path = path.replaceAll("/$", "");
            path = path.replaceAll("^/.*scm", "");
            path = path.replaceAll("^/.*git", "");
            return path;
        }
    }
}

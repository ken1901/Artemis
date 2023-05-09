package de.tum.in.www1.artemis.service;

import java.util.*;

import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.web.rest.dto.*;
import de.tum.in.www1.artemis.web.rest.util.PageUtil;

@Service
public class LearningGoalService {

    private final LearningGoalRepository learningGoalRepository;

    private final AuthorizationCheckService authCheckService;

    private final LearningGoalProgressService learningGoalProgressService;

    public LearningGoalService(LearningGoalRepository learningGoalRepository, AuthorizationCheckService authCheckService, LearningGoalProgressService learningGoalProgressService) {
        this.learningGoalRepository = learningGoalRepository;
        this.authCheckService = authCheckService;
        this.learningGoalProgressService = learningGoalProgressService;
    }

    /**
     * Get all learning goals for a course, including the progress for the user.
     *
     * @param course         The course for which the learning goals should be retrieved.
     * @param user           The user for whom to filter the visible lecture units attached to the learning goal.
     * @param updateProgress Whether the learning goal progress should be updated or taken from the database.
     * @return A list of learning goals with their lecture units (filtered for the user) and user progress.
     */
    public Set<LearningGoal> findAllForCourse(@NotNull Course course, @NotNull User user, boolean updateProgress) {
        if (updateProgress) {
            // Get the learning goals with the updated progress for the specified user.
            return learningGoalProgressService.getLearningGoalsAndUpdateProgressByUserInCourse(user, course);
        }
        else {
            // Fetch the learning goals with the user progress from the database.
            return learningGoalRepository.findAllForCourseWithProgressForUser(course.getId(), user.getId());
        }
    }

    /**
     * Get all prerequisites for a course. Lecture units are removed if the student is not part of the course.
     *
     * @param course The course for which the prerequisites should be retrieved.
     * @param user   The user that is requesting the prerequisites.
     * @return A list of prerequisites (without lecture units if student is not part of course).
     */
    public Set<LearningGoal> findAllPrerequisitesForCourse(@NotNull Course course, @NotNull User user) {
        Set<LearningGoal> prerequisites = learningGoalRepository.findPrerequisitesByCourseId(course.getId());
        // Remove all lecture units
        for (LearningGoal prerequisite : prerequisites) {
            prerequisite.setLectureUnits(Collections.emptySet());
        }
        return prerequisites;
    }

    /**
     * Search for all learning goals fitting a {@link PageableSearchDTO search query}. The result is paged.
     *
     * @param search The search query defining the search term and the size of the returned page
     * @param user   The user for whom to fetch all available lectures
     * @return A wrapper object containing a list of all found learning goals and the total number of pages
     */
    public SearchResultPageDTO<LearningGoal> getAllOnPageWithSize(final PageableSearchDTO<String> search, final User user) {
        final var pageable = PageUtil.createLearningGoalPageRequest(search);
        final var searchTerm = search.getSearchTerm();
        final Page<LearningGoal> learningGoalPage;
        if (authCheckService.isAdmin(user)) {
            learningGoalPage = learningGoalRepository.findByTitleIgnoreCaseContainingOrCourse_TitleIgnoreCaseContaining(searchTerm, searchTerm, pageable);
        }
        else {
            learningGoalPage = learningGoalRepository.findByTitleInLectureOrCourseAndUserHasAccessToCourse(searchTerm, searchTerm, user.getGroups(), pageable);
        }
        return new SearchResultPageDTO<>(learningGoalPage.getContent(), learningGoalPage.getTotalPages());
    }

    /**
     * Checks if the provided learning goals and relations between them contain a cycle
     *
     * @param learningGoals The set of learning goals that get checked for cycles
     * @param relations     The set of relations that get checked for cycles
     * @return A boolean that states whether the provided learning goals and relations contain a cycle
     */
    public boolean doesCreateCircularRelation(Set<LearningGoal> learningGoals, Set<LearningGoalRelation> relations) {
        // Inner class Vertex is only used in this method for cycle detection
        class Vertex {

            private final String label;

            private boolean beingVisited;

            private boolean visited;

            private final Set<Vertex> adjacencyList;

            public Vertex(String label) {
                this.label = label;
                this.adjacencyList = new HashSet<>();
            }

            public Set<Vertex> getAdjacencyList() {
                return adjacencyList;
            }

            public void addNeighbor(Vertex adjacent) {
                this.adjacencyList.add(adjacent);
            }

            public boolean isBeingVisited() {
                return beingVisited;
            }

            public void setBeingVisited(boolean beingVisited) {
                this.beingVisited = beingVisited;
            }

            public boolean isVisited() {
                return visited;
            }

            public void setVisited(boolean visited) {
                this.visited = visited;
            }
        }

        // Inner class Graph is only used in this method for cycle detection
        class Graph {

            private final List<Vertex> vertices;

            public Graph() {
                this.vertices = new ArrayList<>();
            }

            public void addVertex(Vertex vertex) {
                this.vertices.add(vertex);
            }

            public void addEdge(Vertex from, Vertex to) {
                from.addNeighbor(to);
            }

            // Checks all vertices of the graph if they are part of a cycle.
            // This is necessary because otherwise we would not traverse the complete graph if it is not connected
            public boolean hasCycle() {
                for (Vertex vertex : vertices) {
                    if (!vertex.isVisited() && vertexIsPartOfCycle(vertex)) {
                        return true;
                    }
                }
                return false;
            }

            public boolean vertexIsPartOfCycle(Vertex sourceVertex) {
                sourceVertex.setBeingVisited(true);
                for (Vertex neighbor : sourceVertex.getAdjacencyList()) {
                    if (neighbor.isBeingVisited()) {
                        // backward edge exists
                        return true;
                    }
                    else if (!neighbor.isVisited() && vertexIsPartOfCycle(neighbor)) {
                        return true;
                    }
                }
                sourceVertex.setBeingVisited(false);
                sourceVertex.setVisited(true);
                return false;
            }
        }

        var graph = new Graph();
        for (LearningGoal learningGoal : learningGoals) {
            graph.addVertex(new Vertex(learningGoal.getTitle()));
        }
        for (LearningGoalRelation relation : relations) {
            var headVertex = graph.vertices.stream().filter(vertex -> vertex.label.equals(relation.getHeadLearningGoal().getTitle())).findFirst().orElseThrow();
            var tailVertex = graph.vertices.stream().filter(vertex -> vertex.label.equals(relation.getTailLearningGoal().getTitle())).findFirst().orElseThrow();
            // Only EXTENDS and ASSUMES are included in the generated graph as other relations are no problem if they are circular
            // MATCHES relations are considered in the next step by merging the edges and combining the adjacencyLists
            switch (relation.getType()) {
                case EXTENDS, ASSUMES -> graph.addEdge(tailVertex, headVertex);
            }
        }
        // combine vertices that are connected through MATCHES
        for (LearningGoalRelation relation : relations) {
            if (relation.getType() == LearningGoalRelation.RelationType.MATCHES) {
                var headVertex = graph.vertices.stream().filter(vertex -> vertex.label.equals(relation.getHeadLearningGoal().getTitle())).findFirst().orElseThrow();
                var tailVertex = graph.vertices.stream().filter(vertex -> vertex.label.equals(relation.getTailLearningGoal().getTitle())).findFirst().orElseThrow();
                if (headVertex.adjacencyList.contains(tailVertex) || tailVertex.adjacencyList.contains(headVertex)) {
                    return true;
                }
                // create a merged vertex
                var mergedVertex = new Vertex(tailVertex.label + ", " + headVertex.label);
                // add all neighbours to merged vertex
                mergedVertex.getAdjacencyList().addAll(headVertex.getAdjacencyList());
                mergedVertex.getAdjacencyList().addAll(tailVertex.getAdjacencyList());
                // update every vertex that initially had one of the two merged vertices as neighbours to now reference the merged vertex
                for (Vertex vertex : graph.vertices) {
                    for (Vertex adjacentVertex : vertex.getAdjacencyList()) {
                        if (adjacentVertex.label.equals(headVertex.label) || adjacentVertex.label.equals(tailVertex.label)) {
                            vertex.getAdjacencyList().remove(adjacentVertex);
                            vertex.getAdjacencyList().add(mergedVertex);
                        }
                    }
                }
            }
        }
        return graph.hasCycle();
    }
}

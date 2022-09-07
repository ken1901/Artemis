package de.tum.in.www1.artemis.service.scheduled.distributed.callables.programming;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.hazelcast.spring.context.SpringAware;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.service.programming.ProgrammingExerciseImportService;

@SpringAware
public class ImportProgrammingExerciseCallable implements Callable<ProgrammingExercise>, java.io.Serializable {

    private transient ProgrammingExerciseImportService programmingExerciseImportService;

    private final SecurityContext securityContext;

    private final ProgrammingExercise originalProgrammingExercise;

    private final ProgrammingExercise newExercise;

    private final Boolean updateTemplate;

    private final Boolean recreateBuildPlans;

    public ImportProgrammingExerciseCallable(ProgrammingExercise originalProgrammingExercise, SecurityContext securityContext, ProgrammingExercise newExercise,
            boolean updateTemplate, boolean recreateBuildPlans) {
        this.originalProgrammingExercise = originalProgrammingExercise;
        this.securityContext = securityContext;
        this.newExercise = newExercise;
        this.updateTemplate = updateTemplate;
        this.recreateBuildPlans = recreateBuildPlans;
    }

    @Override
    public ProgrammingExercise call() {
        SecurityContextHolder.setContext(securityContext);
        return programmingExerciseImportService.importProgrammingExercise(originalProgrammingExercise, newExercise, updateTemplate, recreateBuildPlans);
    }

    @Autowired
    public void setProgrammingExerciseImportService(final ProgrammingExerciseImportService programmingExerciseImportService) {
        this.programmingExerciseImportService = programmingExerciseImportService;
    }
}
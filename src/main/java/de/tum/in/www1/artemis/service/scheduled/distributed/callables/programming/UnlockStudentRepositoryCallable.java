package de.tum.in.www1.artemis.service.scheduled.distributed.callables.programming;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.spring.context.SpringAware;

import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.service.programming.ProgrammingExerciseParticipationService;

@SpringAware
public class UnlockStudentRepositoryCallable implements Callable<Void>, java.io.Serializable {

    private transient ProgrammingExerciseParticipationService programmingExerciseParticipationService;

    private final ProgrammingExercise programmingExercise;

    private final ProgrammingExerciseStudentParticipation participation;

    public UnlockStudentRepositoryCallable(ProgrammingExercise programmingExercise, ProgrammingExerciseStudentParticipation participation) {
        this.programmingExercise = programmingExercise;
        this.participation = participation;
    }

    @Override
    public Void call() {
        programmingExerciseParticipationService.unlockStudentRepository(programmingExercise, participation);
        return null;
    }

    @Autowired
    public void setProgrammingExerciseParticipationService(final ProgrammingExerciseParticipationService programmingExerciseParticipationService) {
        this.programmingExerciseParticipationService = programmingExerciseParticipationService;
    }
}
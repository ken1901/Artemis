package de.tum.in.www1.artemis.security.localvc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;

import de.tum.in.www1.artemis.exception.localvc.LocalVCBadRequestException;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationPushService;

/**
 * Contains an onPostReceive method that is called by JGit after a push has been received (i.e. after the pushed files were successfully written to disk).
 */
public class LocalVCPostPushHook implements PostReceiveHook {

    private final Optional<ContinuousIntegrationPushService> continuousIntegrationPushService;

    public LocalVCPostPushHook(Optional<ContinuousIntegrationPushService> continuousIntegrationPushService) {
        this.continuousIntegrationPushService = continuousIntegrationPushService;
    }

    @Override
    public void onPostReceive(ReceivePack rp, Collection<ReceiveCommand> commands) throws LocalVCBadRequestException {
        Iterator<ReceiveCommand> iterator = commands.iterator();

        // There should at least be one command.
        if (!iterator.hasNext()) {
            return;
        }

        ReceiveCommand command = iterator.next();

        // There should only be one command.
        if (iterator.hasNext()) {
            command.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON, "There should only be one command.");
            return;
        }

        if (command.getType() != ReceiveCommand.Type.UPDATE) {
            command.setResult(ReceiveCommand.Result.REJECTED_OTHER_REASON, "Only update commands are allowed.");
            return;
        }

        String commitHash = command.getNewId().name();

        Repository repository = rp.getRepository();

        continuousIntegrationPushService.ifPresent(service -> service.processNewPush(commitHash, repository));
    }
}

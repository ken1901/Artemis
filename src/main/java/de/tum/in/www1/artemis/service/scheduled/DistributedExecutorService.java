package de.tum.in.www1.artemis.service.scheduled;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.stereotype.Service;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MemberSelector;
import com.hazelcast.core.HazelcastInstance;

import dev.failsafe.Failsafe;
import dev.failsafe.Fallback;
import dev.failsafe.RetryPolicy;

@Service
public class DistributedExecutorService {

    private final Logger log = LoggerFactory.getLogger(DistributedExecutorService.class);

    private final HazelcastInstance hazelcastInstance;

    private final DiscoveryClient discoveryClient;

    private final Optional<Registration> registration;

    public DistributedExecutorService(HazelcastInstance hazelcastInstance, DiscoveryClient discoveryClient, Optional<Registration> registration) {
        this.hazelcastInstance = hazelcastInstance;
        this.discoveryClient = discoveryClient;
        this.registration = registration;
    }

    public <T> T executeTaskOnMemberWithProfile(Callable<T> taskCallable, String profile) {
        /*
         * if (this.registration.isEmpty()) { // No distributed setup -> This instance has to execute it // TODO return null; } var instances =
         * discoveryClient.getInstances(registration.get().getServiceId()); var instancesWithMatchingProfile = instances.stream().filter(instance ->
         * Arrays.asList(instance.getMetadata().getOrDefault("profile", "").split(",")).contains(profile)) .toList(); log.info("Instances with profile {} are {}", profile,
         * instancesWithMatchingProfile.stream().map(ServiceInstance::getInstanceId).collect(Collectors.toList())); var hazelcastInstancesWithMatchingProfile =
         * hazelcastInstance.getCluster().getMembers().stream() .filter(member -> Arrays.asList(member.getAttributes().getOrDefault("profile",
         * "").split(",")).contains(profile)).toList(); log.info("Hazelcast Instances with profile {} are {}", profile,
         * hazelcastInstancesWithMatchingProfile.stream().map(Member::getAddress).collect(Collectors.toList())); log.info("Hazelcast members are {}",
         * hazelcastInstance.getCluster().getMembers().stream() .map(m -> "Address: %s, Attributes %s".formatted(m.getAddress(),
         * m.getAttributes().toString())).collect(Collectors.toList()));
         */

        RetryPolicy<Object> retryPolicy = RetryPolicy.builder().handle(ExecutionException.class).withBackoff(1, 30, ChronoUnit.SECONDS)
                .onFailedAttempt(e -> log.error("Connection attempt failed", e.getLastException())).onRetry(e -> log.warn("Failure #{}. Retrying.", e.getAttemptCount()))
                .onRetriesExceeded(e -> log.warn("Failed to connect. Max retries exceeded.")).build();

        Fallback<Object> fallbackException = Fallback.ofException(executionAttemptedEvent -> {
            System.err.println("Fallback called");
            throw executionAttemptedEvent.getLastException().getCause();
        });

        try {
            return Failsafe.with(fallbackException).compose(retryPolicy)
                    .getAsync(() -> hazelcastInstance.getExecutorService("test").submit(taskCallable, new ProfileMemberSelector(profile)).get()).get();
        }
        catch (ExecutionException e) {
            log.error("Error during execution of task", e);
            throw new RuntimeException(e.getCause());
        }
        catch (InterruptedException e) {
            log.error("Interrupted during execution of task", e);
            throw new RuntimeException(e.getCause());
        }
    }

    static class ProfileMemberSelector implements MemberSelector {

        String profile;

        ProfileMemberSelector(String profile) {
            this.profile = profile;
        }

        @Override
        public boolean select(Member member) {
            return Arrays.asList(member.getAttributes().getOrDefault("profiles", "").split(",")).contains(profile);
        }
    }

}
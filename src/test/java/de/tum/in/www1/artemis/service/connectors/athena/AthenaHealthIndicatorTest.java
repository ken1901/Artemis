package de.tum.in.www1.artemis.service.connectors.athena;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.connector.AthenaRequestMockProvider;

class AthenaHealthIndicatorTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    private AthenaRequestMockProvider athenaRequestMockProvider;

    @Autowired
    private AthenaHealthIndicator athenaHealthIndicator;

    @BeforeEach
    void initTestCase() {
        athenaRequestMockProvider.enableMockingOfRequests();
    }

    @AfterEach
    void tearDown() throws Exception {
        athenaRequestMockProvider.reset();
    }

    @Test
    void healthUp() {
        athenaRequestMockProvider.mockHealthStatus(true);
        final Health health = athenaHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void healthDown() {
        athenaRequestMockProvider.mockHealthStatus(false);
        final Health health = athenaHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}

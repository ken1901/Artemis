package de.tum.in.www1.artemis.connector;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.net.SocketTimeoutException;

import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.client.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
@Profile("athena")
public class AthenaRequestMockProvider {

    private final RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private final RestTemplate shortTimeoutRestTemplate;

    private MockRestServiceServer mockServerShortTimeout;

    @Value("${artemis.athena.url}")
    private String athenaUrl;

    @Autowired
    private ObjectMapper mapper;

    private AutoCloseable closeable;

    public AthenaRequestMockProvider(@Qualifier("athenaRestTemplate") RestTemplate restTemplate,
            @Qualifier("shortTimeoutAthenaRestTemplate") RestTemplate shortTimeoutRestTemplate) {
        this.restTemplate = restTemplate;
        this.shortTimeoutRestTemplate = shortTimeoutRestTemplate;
    }

    public void enableMockingOfRequests() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServerShortTimeout = MockRestServiceServer.createServer(shortTimeoutRestTemplate);
        closeable = MockitoAnnotations.openMocks(this);
    }

    public void reset() throws Exception {
        if (mockServer != null) {
            mockServer.reset();
        }

        if (mockServerShortTimeout != null) {
            mockServerShortTimeout.reset();
        }

        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Mocks the /submissions API from Athena used to submit all submissions of an exercise
     */
    public void mockSendSubmissionsAndExpect(RequestMatcher... expectedContents) {
        final ObjectNode node = mapper.createObjectNode();
        node.set("data", null);
        node.set("module_name", mapper.valueToTree("module_example"));
        node.set("status", mapper.valueToTree(200));
        final String json = node.toString();

        ResponseActions responseActions = mockServer.expect(ExpectedCount.once(), requestTo(athenaUrl + "/modules/text/module_text_cofee/submissions"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().contentType(MediaType.APPLICATION_JSON));

        for (RequestMatcher matcher : expectedContents) {
            responseActions.andExpect(matcher);
        }

        responseActions.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    /**
     * Mocks /health API success from Athena used to check if the service is up and running
     *
     * @param exampleModuleHealthy Example module health status (in addition to the general status)
     */
    public void mockHealthStatusSuccess(boolean exampleModuleHealthy) {
        final ResponseActions responseActions = mockServerShortTimeout.expect(ExpectedCount.once(), requestTo(athenaUrl + "/health")).andExpect(method(HttpMethod.GET));

        final ObjectNode node = mapper.createObjectNode();
        // Response: {"status":"ok","modules":{"module_example":{"url":"http://localhost:5001","type":"programming","healthy":true}}
        node.set("status", mapper.valueToTree("ok"));
        final ObjectNode modules = mapper.createObjectNode();
        final ObjectNode moduleExample = mapper.createObjectNode();
        moduleExample.set("url", mapper.valueToTree("http://localhost:5001"));
        moduleExample.set("type", mapper.valueToTree("programming"));
        moduleExample.set("healthy", mapper.valueToTree(exampleModuleHealthy));
        modules.set("module_example", moduleExample);
        node.set("modules", modules);
        final String json = node.toString();
        responseActions.andRespond(withSuccess().body(json).contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Mocks /health API failure from Athena used to check if the service is up and running
     */
    public void mockHealthStatusFailure() {
        final ResponseActions responseActions = mockServerShortTimeout.expect(ExpectedCount.once(), requestTo(athenaUrl + "/health")).andExpect(method(HttpMethod.GET));
        responseActions.andRespond(withException(new SocketTimeoutException()));
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}

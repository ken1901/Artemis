package de.tum.in.www1.artemis.service.iris;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.domain.iris.IrisMessage;
import de.tum.in.www1.artemis.domain.iris.IrisMessageContent;
import de.tum.in.www1.artemis.domain.iris.IrisMessageSender;
import de.tum.in.www1.artemis.domain.iris.IrisSession;

@Service
public class IrisGPT3_5Service implements IrisModel {

    private final Logger log = LoggerFactory.getLogger(IrisGPT3_5Service.class);

    /**
     * The RestTemplate used to send requests to the LLM API.
     * TODO: Needs to be configured?
     */
    private final RestTemplate restTemplate;

    /**
     * The URL to send the API requests to. Defaults to <a href="https://api.openai.com/v1/chat/completions">...</a>.
     */
    @Value("${artemis.iris.models.gpt3_5.url:https://api.openai.com/v1/chat/completions}")
    private URL apiURL;

    /**
     * The API key to use when sending requests to the API.
     */
    @Value("${artemis.iris.models.gpt3_5.api-key}")
    private String apiKey;

    /**
     * The ID of the model to request from the API. Defaults to "gpt-3.5-turbo".
     */
    @Value("${artemis.iris.models.gpt3_5.model:gpt-3.5-turbo}")
    private String model;

    /**
     * The temperature parameter controls the randomness of the model. Lower temperatures make the model more
     * deterministic, while higher temperatures make the model more varied in its responses. For a more detailed
     * explanation, see <a href="https://platform.openai.com/docs/api-reference/completions/create">...</a>
     */
    @Value("${artemis.iris.models.gpt3_5.temperature}")
    private float temperature;

    /**
     * The maximum number of tokens that the model should generate in its responses. This is a safety measure to prevent
     * the model from returning too much data.
     */
    @Value("${artemis.iris.models.gpt3_5.max-generated-tokens}")
    private int maxGeneratedTokens;

    /**
     * Up to four sequences of characters that the model should stop generating text at. This is a safety measure to
     * prevent the model from generating too much text.
     */
    @Value("${artemis.iris.models.gpt3_5.stop-sequences}")
    private String[] stopSequences;

    public IrisGPT3_5Service(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public CompletableFuture<String> getResponse(IrisSession session) {
        String jsonBody;
        try {
            Map<String, Object> requestParams = createRequestParams(session.getMessages());
            jsonBody = new ObjectMapper().writeValueAsString(requestParams);
        }
        catch (JsonProcessingException e) {
            log.warn("Error while converting request parameters to JSON.", e);
            return CompletableFuture.failedFuture(e);
        }
        HttpEntity<String> request = new HttpEntity<>(jsonBody, createHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(apiURL.toString(), request, String.class);
        return CompletableFuture.completedFuture(response.getBody());
    }

    /**
     * Gets a {@link Map} of request parameters to be sent to this LLM API. These parameters will be converted into a
     * JSON object and sent in the request body.
     * <p>
     * Different models require different request parameters, so this method must be overridden by subclasses to create
     * the parameters for the specific model.
     * <p>
     * Subclasses must use the provided conversation history so that the model can respond to the user's previous
     * messages. The system instructions should be included in the parameters as instructions to the model.
     *
     * @param conversationHistory The messages that have been sent in the conversation so far
     * @return The API request parameters
     */
    private Map<String, Object> createRequestParams(List<IrisMessage> conversationHistory) {
        return Map.of("model", model, "messages", convertMessages(conversationHistory), "temperature", temperature, "max_tokens", maxGeneratedTokens, "stop", stopSequences);
    }

    /**
     * Converts the system instructions and conversation history into GPT3_5's message format.
     *
     * @param conversationHistory The conversation history to convert.
     * @return The converted messages.
     */
    private static List<Map<String, String>> convertMessages(List<IrisMessage> conversationHistory) {
        // TODO: Sanitize user input.
        return conversationHistory.stream().map(IrisGPT3_5Service::toRoleMessage).toList();
    }

    /**
     * Converts an IrisMessage into GPT3_5's message format, a map with the keys "role" and "message". The value of
     * "role" is either "user", "assistant" or "system", depending on the sender of the message.
     *
     * @param message The message to convert.
     * @return The converted message.
     */
    private static Map<String, String> toRoleMessage(IrisMessage message) {
        return Map.of("role", toRole(message.getSender()), "message", joinContents(message.getContent()));
    }

    /**
     * Converts an IrisMessageSender into GPT3_5's role format, either "user", "assistant" or "system".
     *
     * @param sender The sender to convert.
     * @return The equivalent role, either "user", "assistant" or "system".
     */
    private static String toRole(IrisMessageSender sender) {
        return switch (sender) {
            case USER -> "user";
            case LLM -> "assistant";
            case ARTEMIS -> "system"; // TODO: Remove this case as system instructions do not constitute a message.
        };
    }

    /**
     * Joins the text contents of a list of IrisMessageContent into a single string.
     * TODO: This needs to be reconsidered when we add support for other content types.
     *
     * @param messageContents The message contents to join.
     * @return The joined string.
     */
    private static String joinContents(List<IrisMessageContent> messageContents) {
        return messageContents.stream().map(IrisMessageContent::getTextContent).collect(Collectors.joining("\n"));
    }

    /**
     * Creates a HttpHeaders object with the Content-Type set to application/json and the Authorization header set to
     * the API key, which varies depending on the model used.
     *
     * @return A HttpHeaders object to be used in a request to a LLM API
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

}

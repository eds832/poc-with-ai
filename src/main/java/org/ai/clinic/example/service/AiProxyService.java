package org.ai.clinic.example.service;

import org.ai.clinic.example.config.AiProxyProperties;
import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


/**
 * Calls the EPAM AI proxy chat/completions endpoint.
 *
 * <p>Equivalent of:
 * <pre>
 * curl -X POST "$AI_PROXY_BASE_URL/openai/deployments/$AI_PROXY_DEPLOYMENT/chat/completions" \
 *   -H "Api-Key: $AI_PROXY_API_KEY" \
 *   -H "Content-Type:application/json" \
 *   -d '{"messages": [{"role": "user", "content": "My content"}]}'
 * </pre>
 *
 * <p>The host is never hardcoded; it comes from {@code AI_PROXY_BASE_URL}.
 */
@Service
public class AiProxyService {

    private static final Logger logger = LoggerFactory.getLogger(AiProxyService.class);

    private final RestClient restClient;
    private final AiProxyProperties properties;

    public AiProxyService(@Qualifier("aiProxyRestClient") RestClient restClient, AiProxyProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Sends a full chat completion request and returns the raw (deserialized) response.
     */
    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        String path = properties.chatCompletionsPath();
        logger.info("Calling AI proxy: POST {}{}", properties.getBaseUrl(), path);
        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        throw new AiProxyException(
                                "AI proxy call failed with status %s: %s".formatted(res.getStatusCode(), body));
                    })
                    .body(ChatCompletionResponse.class);
        } catch (RestClientException e) {
            throw new AiProxyException("Unable to call AI proxy: " + e.getMessage(), e);
        }

        if (response == null) {
            throw new AiProxyException("AI proxy returned an empty body");
        }
        return response;
    }
}

package org.ai.clinic.example.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration for the EPAM AI Proxy endpoint.
 *
 * <p>Resolved URL: {@code {baseUrl}/openai/deployments/{deployment}/chat/completions}
 *
 * <p>Values are supplied through environment variables (see {@code application.properties}):
 * {@code AI_PROXY_BASE_URL} (required), {@code AI_PROXY_DEPLOYMENT}, {@code AI_PROXY_API_KEY},
 * {@code AI_PROXY_TIMEOUT}. No host is hardcoded.
 */
@ConfigurationProperties(prefix = "ai-proxy")
@Validated
public class AiProxyProperties {

    /** Base URL of the AI proxy, injected from {@code AI_PROXY_BASE_URL}. */
    @NotBlank(message = "ai-proxy.base-url must be set (environment variable AI_PROXY_BASE_URL)")
    private String baseUrl;

    /** Deployment (model) name, e.g. {@code anthropic.claude-opus-5}. */
    private String deployment = "anthropic.claude-opus-5";

    /** Value sent in the {@code Api-Key} header. */
    private String apiKey;

    /** Read/connect timeout for calls to the proxy. */
    private Duration timeout = Duration.ofSeconds(60);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String chatCompletionsPath() {
        return "/openai/deployments/" + deployment + "/chat/completions";
    }
}

package org.ai.clinic.example.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProxyProperties.class)
public class AiProxyConfig {

    /**
     * Dedicated {@link RestClient} for the AI proxy with its own timeouts and the {@code Api-Key} header.
     */
    @Bean
    public RestClient aiProxyRestClient(RestClient.Builder builder, AiProxyProperties properties) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect()
                .build(HttpClientSettings.defaults()
                        .withTimeouts(properties.getTimeout(), properties.getTimeout()));

        return builder.clone()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Api-Key", properties.getApiKey())
                .build();
    }
}

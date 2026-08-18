package org.ai.clinic.example.service;

import org.ai.clinic.example.config.AiProxyProperties;
import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiProxyServiceTest {

    private static final String BASE_URL = "https://ai-proxy.example.test";
    private static final String EXPECTED_URL =
            BASE_URL + "/openai/deployments/anthropic.claude-v3-haiku/chat/completions";

    private MockRestServiceServer server;
    private AiProxyService service;

    @BeforeEach
    void setUp() {
        AiProxyProperties properties = new AiProxyProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey("my-api-key");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Api-Key", properties.getApiKey());

        server = MockRestServiceServer.bindTo(builder).build();
        service = new AiProxyService(builder.build(), properties);
    }

    @Test
    void completeSendsCurlEquivalentRequestAndReturnsResponse() {
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Api-Key", "my-api-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("My content"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-1",
                          "model": "anthropic.claude-opus-5",
                          "choices": [
                            {"index": 0, "message": {"role": "assistant", "content": "Hello!"}, "finish_reason": "stop"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ChatCompletionResponse response = service.complete(ChatCompletionRequest.ofUserMessage("My content"));
        assertEquals("Hello!", response.firstContent());
        server.verify();
    }

    @Test
    void errorStatusIsWrappedInAiProxyException() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withServerError().body("boom"));

        AiProxyException e = assertThrows(AiProxyException.class,
                () -> service.complete(ChatCompletionRequest.ofUserMessage("My content")));
        assertTrue(e.getMessage().contains("500"));
    }
}

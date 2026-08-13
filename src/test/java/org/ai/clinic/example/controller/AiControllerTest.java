package org.ai.clinic.example.controller;

import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.ai.clinic.example.dto.ChatMessage;
import org.ai.clinic.example.service.AiProxyException;
import org.ai.clinic.example.service.AiProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiController.class)
@Import(GlobalExceptionHandler.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiProxyService aiProxyService;

    @Test
    void askReturnsAnswerFromService() throws Exception {
        ChatCompletionResponse stub = new ChatCompletionResponse(
                "chatcmpl-1",
                "anthropic.claude-opus-5",
                List.of(new ChatCompletionResponse.Choice(0, new ChatMessage("assistant", "42"), "stop")),
                null);
        given(aiProxyService.complete(any(ChatCompletionRequest.class))).willReturn(stub);

        mockMvc.perform(get("/ai/ask").param("query", "What is the answer?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("What is the answer?"))
                .andExpect(jsonPath("$.answer").value("42"))
                .andExpect(jsonPath("$.model").value("anthropic.claude-opus-5"));
    }

    @Test
    void askTextReturnsPlainText() throws Exception {
        given(aiProxyService.ask("hi")).willReturn("hello");

        mockMvc.perform(get("/ai/ask/text").param("query", "hi"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));
    }

    @Test
    void missingQueryParamIsBadRequest() throws Exception {
        mockMvc.perform(get("/ai/ask"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void proxyFailureIsBadGateway() throws Exception {
        given(aiProxyService.complete(any(ChatCompletionRequest.class)))
                .willThrow(new AiProxyException("upstream down"));

        mockMvc.perform(get("/ai/ask").param("query", "hi"))
                .andExpect(status().isBadGateway());
    }
}

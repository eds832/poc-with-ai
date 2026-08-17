package org.ai.clinic.example.controller;

import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.ai.clinic.example.dto.ChatMessage;
import org.ai.clinic.example.service.ClinicChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClinicChatController.class)
class ClinicChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClinicChatService clinicChatService;

    @Test
    void ask_returnsJsonWithQueryAndAnswer() throws Exception {
        ChatCompletionResponse response = responseWith("Dr. Smith is available.");
        when(clinicChatService.complete(any())).thenReturn(response);

        mockMvc.perform(get("/clinic/ask").param("query", "When is Dr. Smith available?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("When is Dr. Smith available?"))
                .andExpect(jsonPath("$.answer").value("Dr. Smith is available."))
                .andExpect(jsonPath("$.model").value("test-model"));
    }

    @Test
    void ask_missingQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/clinic/ask"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askText_returnsPlainText() throws Exception {
        when(clinicChatService.ask("What are your hours?")).thenReturn("Mon-Fri 8-6.");

        mockMvc.perform(get("/clinic/ask/text").param("query", "What are your hours?"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string("Mon-Fri 8-6."));
    }

    @Test
    void chat_acceptsJsonBody_returnsResponse() throws Exception {
        ChatCompletionResponse response = responseWith("Hello! How can I help you?");
        when(clinicChatService.complete(any())).thenReturn(response);

        mockMvc.perform(post("/clinic/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages": [{"role": "user", "content": "Hi"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choices[0].message.content").value("Hello! How can I help you?"));
    }

    @Test
    void chat_emptyBody_returns400() throws Exception {
        when(clinicChatService.complete(any()))
                .thenThrow(new IllegalArgumentException("messages must not be empty"));

        mockMvc.perform(post("/clinic/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ask_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(clinicChatService.complete(any()))
                .thenThrow(new IllegalArgumentException("query must not be empty"));

        mockMvc.perform(get("/clinic/ask").param("query", ""))
                .andExpect(status().isBadRequest());
    }

    private static ChatCompletionResponse responseWith(String content) {
        return new ChatCompletionResponse(
                "chatcmpl-test",
                "test-model",
                List.of(new ChatCompletionResponse.Choice(
                        0,
                        new ChatMessage("assistant", content),
                        "stop")),
                null);
    }
}

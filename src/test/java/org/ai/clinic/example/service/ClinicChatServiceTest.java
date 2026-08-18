package org.ai.clinic.example.service;

import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.ai.clinic.example.dto.ChatMessage;
import org.ai.clinic.example.repository.QueryExecutor.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicChatServiceTest {

    private AiProxyService aiProxyService;
    private SqlQueryService sqlQueryService;
    private PolicyService policyService;
    private ClinicChatService service;

    @BeforeEach
    void setUp() {
        aiProxyService = mock(AiProxyService.class);
        sqlQueryService = mock(SqlQueryService.class);
        policyService = mock(PolicyService.class);
        service = new ClinicChatService(aiProxyService, sqlQueryService, policyService);

        when(policyService.getPolicyText()).thenReturn("Clinic hours: Mon-Fri 8-6.");
        when(sqlQueryService.getSchemaDescription()).thenReturn("Table: doctors\nTable: slots");
    }

    @Test
    void ask_blankQuery_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.ask("  "));
    }

    @Test
    void ask_nullQuery_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.ask(null));
    }

    @Test
    void complete_nullMessages_throws() {
        ChatCompletionRequest request = new ChatCompletionRequest(null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.complete(request));
    }

    @Test
    void complete_emptyMessages_throws() {
        ChatCompletionRequest request = new ChatCompletionRequest(List.of(), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.complete(request));
    }

    @Test
    void complete_noneResponse_skipsDbAndReturnsAnswer() {
        ChatCompletionResponse sqlResponse = responseWith("NONE");
        ChatCompletionResponse finalResponse = responseWith("Our clinic is open Mon-Fri 8-6.");

        when(aiProxyService.complete(any())).thenReturn(sqlResponse, finalResponse);

        ChatCompletionResponse result = service.complete(ChatCompletionRequest.ofUserMessage("What are your hours?"));

        assertEquals("Our clinic is open Mon-Fri 8-6.", result.firstContent());
        verify(sqlQueryService, never()).executeSelect(any());
        verify(aiProxyService, times(2)).complete(any());
    }

    @Test
    void complete_withSql_executesQueryAndIncludesInContext() {
        ChatCompletionResponse sqlResponse = responseWith("SELECT * FROM DOCTORS WHERE UPPER(NAME) LIKE UPPER('%Smith%')");
        ChatCompletionResponse finalResponse = responseWith("Dr. Smith is available tomorrow at 9am.");

        when(aiProxyService.complete(any())).thenReturn(sqlResponse, finalResponse);
        when(sqlQueryService.executeSelect(any())).thenReturn(
                new QueryResult(List.of(Map.of("NAME", "Dr. Smith", "SPECIALIZATION", "Orthodontist")), false));

        ChatCompletionResponse result = service.complete(ChatCompletionRequest.ofUserMessage("Is Dr. Smith available?"));

        assertEquals("Dr. Smith is available tomorrow at 9am.", result.firstContent());
        verify(sqlQueryService).executeSelect("SELECT * FROM DOCTORS WHERE UPPER(NAME) LIKE UPPER('%Smith%')");
    }

    @Test
    void complete_sqlExecutionFails_retriesOnce() {
        ChatCompletionResponse sqlResponse = responseWith("SELECT * FROM doctors");
        ChatCompletionResponse fixedSqlResponse = responseWith("SELECT * FROM DOCTORS");
        ChatCompletionResponse finalResponse = responseWith("Here are the doctors.");

        when(aiProxyService.complete(any()))
                .thenReturn(sqlResponse)
                .thenReturn(fixedSqlResponse)
                .thenReturn(finalResponse);

        when(sqlQueryService.executeSelect("SELECT * FROM doctors"))
                .thenThrow(new RuntimeException("Syntax error"));
        when(sqlQueryService.executeSelect("SELECT * FROM DOCTORS"))
                .thenReturn(new QueryResult(List.of(Map.of("NAME", "Dr. Smith")), false));

        ChatCompletionResponse result = service.complete(ChatCompletionRequest.ofUserMessage("List doctors"));

        assertEquals("Here are the doctors.", result.firstContent());
        verify(aiProxyService, times(3)).complete(any());
    }

    @Test
    void complete_sqlExecutionFailsTwice_returnsGracefulFallback() {
        ChatCompletionResponse sqlResponse = responseWith("SELECT bad_sql");
        ChatCompletionResponse fixedSqlResponse = responseWith("SELECT still_bad");
        ChatCompletionResponse finalResponse = responseWith("I couldn't find that info.");

        when(aiProxyService.complete(any()))
                .thenReturn(sqlResponse)
                .thenReturn(fixedSqlResponse)
                .thenReturn(finalResponse);

        when(sqlQueryService.executeSelect(any()))
                .thenThrow(new RuntimeException("Syntax error"));

        ChatCompletionResponse result = service.complete(ChatCompletionRequest.ofUserMessage("Find something"));

        assertEquals("I couldn't find that info.", result.firstContent());
    }

    @Test
    void ask_returnsPlainTextContent() {
        ChatCompletionResponse sqlResponse = responseWith("NONE");
        ChatCompletionResponse finalResponse = responseWith("Hello!");

        when(aiProxyService.complete(any())).thenReturn(sqlResponse, finalResponse);

        String answer = service.ask("Hi");
        assertEquals("Hello!", answer);
    }

    @Test
    void ask_nullContent_throws() {
        ChatCompletionResponse sqlResponse = responseWith("NONE");
        ChatCompletionResponse emptyResponse = new ChatCompletionResponse(
                "id", "model", List.of(), null);

        when(aiProxyService.complete(any())).thenReturn(sqlResponse, emptyResponse);

        assertThrows(AiProxyException.class, () -> service.ask("Hello"));
    }

    @Test
    void complete_multiTurnConversation_usesFullHistory() {
        List<ChatMessage> messages = List.of(
                ChatMessage.user("Who is available tomorrow?"),
                new ChatMessage("assistant", "Dr. Smith and Dr. Johnson are available."),
                ChatMessage.user("What about the day after?")
        );

        ChatCompletionResponse sqlResponse = responseWith("SELECT * FROM SLOTS");
        ChatCompletionResponse finalResponse = responseWith("Dr. Williams is available.");

        when(aiProxyService.complete(any())).thenReturn(sqlResponse, finalResponse);
        when(sqlQueryService.executeSelect(any())).thenReturn(new QueryResult(List.of(), false));

        ChatCompletionRequest request = new ChatCompletionRequest(messages, null, null);
        ChatCompletionResponse result = service.complete(request);

        assertEquals("Dr. Williams is available.", result.firstContent());
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

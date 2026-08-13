package org.ai.clinic.example.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Body sent to the proxy: {@code {"messages": [{"role": "user", "content": "My content"}]}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        List<ChatMessage> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens) {

    public static ChatCompletionRequest ofUserMessage(String content) {
        return new ChatCompletionRequest(List.of(ChatMessage.user(content)), null, null);
    }
}

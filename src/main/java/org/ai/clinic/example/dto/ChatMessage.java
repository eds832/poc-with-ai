package org.ai.clinic.example.dto;

/**
 * Single chat message: {@code {"role": "user", "content": "My content"}}.
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }
}

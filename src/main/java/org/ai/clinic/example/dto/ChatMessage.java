package org.ai.clinic.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Single chat message: {@code {"role": "user", "content": "My content"}}.
 */
public record ChatMessage(
        @NotBlank @Pattern(regexp = "user|assistant|system") String role,
        @NotBlank @Size(max = 4000) String content) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }
}

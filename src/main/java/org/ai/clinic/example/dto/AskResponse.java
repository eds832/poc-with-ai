package org.ai.clinic.example.dto;

/**
 * Controller response for {@code /clinic/ask}.
 */
public record AskResponse(String query, String answer, String model) {
}

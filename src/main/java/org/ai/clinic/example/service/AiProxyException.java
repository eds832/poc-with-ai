package org.ai.clinic.example.service;

/**
 * Thrown when the AI proxy call fails or returns an unusable payload.
 */
public class AiProxyException extends RuntimeException {

    public AiProxyException(String message) {
        super(message);
    }

    public AiProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}

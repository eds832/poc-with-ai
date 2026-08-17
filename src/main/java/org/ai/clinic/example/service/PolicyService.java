package org.ai.clinic.example.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the static clinic policy text used as grounding context for the chat.
 *
 * <p>The file is read once at startup through an {@link InputStream} so it also works
 * when the application runs from a packaged JAR (where {@code getFile()} would fail).
 */
@Service
public class PolicyService {

    private static final String POLICY_RESOURCE = "policy.txt";

    private final String policyText;

    public PolicyService() {
        this.policyText = loadPolicyText();
    }

    public String getPolicyText() {
        return policyText;
    }

    private static String loadPolicyText() {
        ClassPathResource resource = new ClassPathResource(POLICY_RESOURCE);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load " + POLICY_RESOURCE + " from the classpath", e);
        }
    }
}

package org.ai.clinic.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyServiceTest {

    @Test
    void loadsPolicyTextFromClasspath() {
        PolicyService service = new PolicyService();
        String text = service.getPolicyText();

        assertFalse(text.isBlank());
        assertTrue(text.contains("CLINIC POLICIES"));
        assertTrue(text.contains("Dr. Emily Smith"));
        assertTrue(text.contains("CANCELLATION POLICY"));
    }

    @Test
    void policyTextContainsAllDoctors() {
        PolicyService service = new PolicyService();
        String text = service.getPolicyText();

        assertTrue(text.contains("Dr. Emily Smith"));
        assertTrue(text.contains("Dr. Michael Johnson"));
        assertTrue(text.contains("Dr. Sarah Williams"));
        assertTrue(text.contains("Dr. David Brown"));
        assertTrue(text.contains("Dr. Laura Davis"));
        assertTrue(text.contains("Dr. Robert Wilson"));
    }

    @Test
    void policyTextContainsClinicHours() {
        PolicyService service = new PolicyService();
        String text = service.getPolicyText();

        assertTrue(text.contains("Monday-Friday"));
        assertTrue(text.contains("Saturday"));
    }
}

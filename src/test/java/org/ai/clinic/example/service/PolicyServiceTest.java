package org.ai.clinic.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyServiceTest {

    private PolicyService service;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql")
                .build();
        service = new PolicyService(new JdbcTemplate(dataSource));
    }

    @Test
    void loadsPolicyTextFromClasspath() {
        String text = service.getPolicyText();

        assertFalse(text.isBlank());
        assertTrue(text.contains("CLINIC POLICIES"));
        assertTrue(text.contains("CANCELLATION POLICY"));
    }

    @Test
    void policyTextContainsAllDoctors() {
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
        String text = service.getPolicyText();

        assertTrue(text.contains("Monday-Friday"));
        assertTrue(text.contains("Saturday"));
    }
}

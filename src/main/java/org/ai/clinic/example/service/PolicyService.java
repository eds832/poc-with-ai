package org.ai.clinic.example.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class PolicyService {

    private static final String POLICY_RESOURCE = "policy.txt";

    private final String policyText;

    public PolicyService(JdbcTemplate jdbcTemplate) {
        String staticPolicy = loadPolicyText();
        String doctorInfo = loadDoctorInfo(jdbcTemplate);
        this.policyText = staticPolicy + "\n" + doctorInfo;
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

    private static String loadDoctorInfo(JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> doctors = jdbcTemplate.queryForList(
                "SELECT name, specialization, experience_years, description FROM doctors ORDER BY id");
        StringBuilder sb = new StringBuilder("DOCTOR INFORMATION:\n");
        for (Map<String, Object> doc : doctors) {
            sb.append("\n%s - %s\n%d years of experience. %s\n".formatted(
                    doc.get("NAME"), doc.get("SPECIALIZATION"),
                    doc.get("EXPERIENCE_YEARS"), doc.get("DESCRIPTION")));
        }
        return sb.toString();
    }
}

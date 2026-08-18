package org.ai.clinic.example.controller;

import org.ai.clinic.example.service.SqlQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
public class DebugController {

    private final SqlQueryService sqlQueryService;

    public DebugController(SqlQueryService sqlQueryService) {
        this.sqlQueryService = sqlQueryService;
    }

    @GetMapping("/debug/doctors")
    public List<Map<String, Object>> debugDoctors() {
        return sqlQueryService.executeSelect("SELECT * FROM doctors").rows();
    }

    @GetMapping("/debug/slots")
    public List<Map<String, Object>> debugSlots() {
        return sqlQueryService.executeSelect("SELECT * FROM slots").rows();
    }
}

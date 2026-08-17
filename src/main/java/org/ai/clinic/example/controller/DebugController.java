package org.ai.clinic.example.controller;

import org.ai.clinic.example.service.SqlQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DebugController {

    private final SqlQueryService sqlQueryService;

    public DebugController(SqlQueryService sqlQueryService) {
        this.sqlQueryService = sqlQueryService;
    }

    /**
     * http://localhost:8080/debug/doctors
     */
    @GetMapping("/debug/doctors")
    public List<Map<String, Object>> debugDoctors() {
        return sqlQueryService.executeSelect("SELECT * FROM doctors");
    }

    /**
     * http://localhost:8080/debug/slots
     */
    @GetMapping("/debug/slots")
    public List<Map<String, Object>> debugSlots() {
        return sqlQueryService.executeSelect("SELECT * FROM slots");
    }
}

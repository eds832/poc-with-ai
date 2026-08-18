package org.ai.clinic.example.controller;

import org.ai.clinic.example.repository.QueryExecutor.QueryResult;
import org.ai.clinic.example.service.SqlQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebugController.class)
@ActiveProfiles("dev")
class DebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SqlQueryService sqlQueryService;

    @Test
    void debugDoctors_returnsDoctorList() throws Exception {
        when(sqlQueryService.executeSelect("SELECT * FROM doctors"))
                .thenReturn(new QueryResult(List.of(
                        Map.of("ID", 1, "NAME", "Dr. Smith", "SPECIALIZATION", "Orthodontist")), false));

        mockMvc.perform(get("/debug/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].NAME").value("Dr. Smith"))
                .andExpect(jsonPath("$[0].SPECIALIZATION").value("Orthodontist"));
    }

    @Test
    void debugSlots_returnsSlotList() throws Exception {
        when(sqlQueryService.executeSelect("SELECT * FROM slots"))
                .thenReturn(new QueryResult(List.of(
                        Map.of("ID", 1, "DOCTOR_ID", 1, "IS_AVAILABLE", true)), false));

        mockMvc.perform(get("/debug/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].DOCTOR_ID").value(1))
                .andExpect(jsonPath("$[0].IS_AVAILABLE").value(true));
    }
}

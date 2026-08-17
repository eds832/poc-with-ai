package org.ai.clinic.example.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorTest {

    private QueryExecutor executor;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql")
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executor = new QueryExecutor(jdbcTemplate);
    }

    @Test
    void execute_selectAllDoctors_returnsRows() {
        List<Map<String, Object>> rows = executor.execute("SELECT * FROM doctors");

        assertFalse(rows.isEmpty());
        assertEquals(6, rows.size());
        assertTrue(rows.getFirst().containsKey("NAME"));
    }

    @Test
    void execute_selectWithFilter_returnsFilteredRows() {
        List<Map<String, Object>> rows = executor.execute(
                "SELECT * FROM doctors WHERE UPPER(name) LIKE UPPER('%smith%')");

        assertEquals(1, rows.size());
        assertEquals("Dr. Emily Smith", rows.getFirst().get("NAME"));
    }

    @Test
    void execute_selectSlots_returnsRows() {
        List<Map<String, Object>> rows = executor.execute("SELECT * FROM slots WHERE is_available = TRUE");

        assertFalse(rows.isEmpty());
        for (Map<String, Object> row : rows) {
            assertEquals(true, row.get("IS_AVAILABLE"));
        }
    }

    @Test
    void execute_respectsMaxRowsCap() {
        StringBuilder insertSql = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            insertSql.append("INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES (1, CURRENT_DATE, '09:00:00', TRUE);\n");
        }
        jdbcTemplate.execute(insertSql.toString());

        List<Map<String, Object>> rows = executor.execute("SELECT * FROM slots");

        assertEquals(200, rows.size());
    }

    @Test
    void execute_emptyResult_returnsEmptyList() {
        List<Map<String, Object>> rows = executor.execute(
                "SELECT * FROM doctors WHERE name = 'NonExistentDoctor'");

        assertTrue(rows.isEmpty());
    }
}

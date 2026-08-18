package org.ai.clinic.example.repository;

import org.ai.clinic.example.repository.QueryExecutor.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
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
        QueryResult result = executor.execute("SELECT * FROM doctors");

        assertFalse(result.rows().isEmpty());
        assertEquals(6, result.rows().size());
        assertTrue(result.rows().getFirst().containsKey("NAME"));
        assertFalse(result.truncated());
    }

    @Test
    void execute_selectWithFilter_returnsFilteredRows() {
        QueryResult result = executor.execute(
                "SELECT * FROM doctors WHERE UPPER(name) LIKE UPPER('%smith%')");

        assertEquals(1, result.rows().size());
        assertEquals("Dr. Emily Smith", result.rows().getFirst().get("NAME"));
        assertFalse(result.truncated());
    }

    @Test
    void execute_selectSlots_returnsRows() {
        QueryResult result = executor.execute("SELECT * FROM slots WHERE is_available = TRUE");

        assertFalse(result.rows().isEmpty());
        for (Map<String, Object> row : result.rows()) {
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

        QueryResult result = executor.execute("SELECT * FROM slots");

        assertEquals(200, result.rows().size());
        assertTrue(result.truncated());
    }

    @Test
    void execute_emptyResult_returnsEmptyList() {
        QueryResult result = executor.execute(
                "SELECT * FROM doctors WHERE name = 'NonExistentDoctor'");

        assertTrue(result.rows().isEmpty());
        assertFalse(result.truncated());
    }
}

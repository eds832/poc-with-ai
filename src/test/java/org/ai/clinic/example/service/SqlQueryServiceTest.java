package org.ai.clinic.example.service;

import org.ai.clinic.example.repository.QueryExecutor;
import org.ai.clinic.example.repository.QueryExecutor.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlQueryServiceTest {

    private QueryExecutor queryExecutor;
    private SqlQueryService service;

    @BeforeEach
    void setUp() {
        queryExecutor = mock(QueryExecutor.class);
        service = new SqlQueryService(queryExecutor);
    }

    @Test
    void executeSelect_validSelect_delegatesToQueryExecutor() {
        List<Map<String, Object>> rows = List.of(Map.of("NAME", "Dr. Smith"));
        when(queryExecutor.execute("SELECT * FROM doctors")).thenReturn(new QueryResult(rows, false));

        QueryResult result = service.executeSelect("SELECT * FROM doctors");

        assertEquals(rows, result.rows());
        assertFalse(result.truncated());
        verify(queryExecutor).execute("SELECT * FROM doctors");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void executeSelect_blankOrNull_throws(String sql) {
        assertThrows(IllegalArgumentException.class, () -> service.executeSelect(sql));
        verify(queryExecutor, never()).execute(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO doctors VALUES (99, 'Hack', 'X', 1, 'X')",
            "DELETE FROM doctors WHERE id = 1",
            "DROP TABLE doctors",
            "CREATE TABLE hack (id INT)",
            "UPDATE doctors SET name = 'Hack' WHERE id = 1",
            "ALTER TABLE doctors ADD COLUMN hack VARCHAR(10)",
            "TRUNCATE TABLE doctors"
    })
    void executeSelect_nonSelect_throws(String sql) {
        assertThrows(IllegalArgumentException.class, () -> service.executeSelect(sql));
        verify(queryExecutor, never()).execute(anyString());
    }

    @Test
    void executeSelect_multipleStatements_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.executeSelect("SELECT 1; DROP TABLE doctors"));
        verify(queryExecutor, never()).execute(anyString());
    }

    @Test
    void executeSelect_selectWithDangerousKeyword_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.executeSelect("SELECT * FROM doctors; DELETE FROM doctors"));
        verify(queryExecutor, never()).execute(anyString());
    }

    @Test
    void executeSelect_selectContainingDangerousSubstring_allowed() {
        // "CREATED_AT" contains "CREATE" as substring, but not as a word boundary — should pass
        when(queryExecutor.execute(anyString())).thenReturn(new QueryResult(List.of(), false));

        service.executeSelect("SELECT CREATED_AT FROM doctors");

        verify(queryExecutor).execute("SELECT CREATED_AT FROM doctors");
    }

    @Test
    void executeSelect_disallowedTable_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.executeSelect("SELECT * FROM some_table"));
        verify(queryExecutor, never()).execute(anyString());
    }

    @Test
    void executeSelect_stripsMarkdownFences() {
        when(queryExecutor.execute(anyString())).thenReturn(new QueryResult(List.of(), false));

        service.executeSelect("```sql\nSELECT * FROM doctors\n```");

        verify(queryExecutor).execute("SELECT * FROM doctors");
    }

    @Test
    void executeSelect_stripsTrailingSemicolon() {
        when(queryExecutor.execute(anyString())).thenReturn(new QueryResult(List.of(), false));

        service.executeSelect("SELECT * FROM doctors;");

        verify(queryExecutor).execute("SELECT * FROM doctors");
    }

    @Test
    void executeSelect_stripsMultipleTrailingSemicolons() {
        when(queryExecutor.execute(anyString())).thenReturn(new QueryResult(List.of(), false));

        service.executeSelect("SELECT * FROM doctors;;;");

        verify(queryExecutor).execute("SELECT * FROM doctors");
    }

    @Test
    void getSchemaDescription_returnsNonEmpty() {
        String schema = service.getSchemaDescription();
        assertTrue(schema.contains("doctors"));
        assertTrue(schema.contains("slots"));
    }
}

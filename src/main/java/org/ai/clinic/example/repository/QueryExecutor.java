package org.ai.clinic.example.repository;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class QueryExecutor {

    /** Upper bound on returned rows, so a broad query cannot blow up the prompt. */
    private static final int MAX_ROWS = 200;

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> execute(String normalizedQuery) {
        return jdbcTemplate.query(
                normalizedQuery,
                rs -> {
                    List<Map<String, Object>> rows = new java.util.ArrayList<>();
                    ColumnMapRowMapper mapper = new ColumnMapRowMapper();
                    int rowNum = 0;
                    while (rs.next() && rowNum < MAX_ROWS) {
                        rows.add(mapper.mapRow(rs, rowNum++));
                    }
                    return rows;
                });
    }
}

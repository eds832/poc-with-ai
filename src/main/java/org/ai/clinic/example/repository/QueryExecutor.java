package org.ai.clinic.example.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class QueryExecutor {

    private static final int MAX_ROWS = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutor(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public QueryResult execute(String normalizedQuery) {
        return jdbcTemplate.execute((Statement stmt) -> {
            stmt.setMaxRows(MAX_ROWS + 1);
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            var rs = stmt.executeQuery(normalizedQuery);
            List<Map<String, Object>> rows = new ArrayList<>();
            ColumnMapRowMapper mapper = new ColumnMapRowMapper();
            int rowNum = 0;
            while (rs.next() && rowNum <= MAX_ROWS) {
                rows.add(mapper.mapRow(rs, rowNum++));
            }
            boolean truncated = rows.size() > MAX_ROWS;
            if (truncated) {
                rows.removeLast();
            }
            return new QueryResult(rows, truncated);
        });
    }

    public record QueryResult(List<Map<String, Object>> rows, boolean truncated) {}
}

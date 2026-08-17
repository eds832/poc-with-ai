package org.ai.clinic.example.service;

import org.ai.clinic.example.repository.QueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Executes the read-only SQL produced by the model against the in-memory clinic database.
 *
 * <p>Because the SQL is model-generated, {@link #executeSelect(String)} acts as a guard rail:
 * it accepts a single {@code SELECT} statement only and rejects anything that could mutate data.
 */
@Service
public class SqlQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SqlQueryService.class);

    private static final Pattern DANGEROUS_KEYWORDS =
            Pattern.compile("\\b(DROP|INSERT|UPDATE|DELETE|CREATE|ALTER|TRUNCATE|EXEC|EXECUTE|MERGE)\\b");

    private final QueryExecutor queryExecutor;

    public SqlQueryService(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    /**
     * Runs a single {@code SELECT} statement and returns the rows.
     *
     * @throws IllegalArgumentException if the statement is blank, is not a {@code SELECT},
     *                                  or contains more than one statement
     */
    public List<Map<String, Object>> executeSelect(String sql) {
        String normalized = normalize(sql);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("SQL statement must not be empty");
        }
        if (!normalized.regionMatches(true, 0, "SELECT", 0, "SELECT".length())) {
            throw new IllegalArgumentException("Only SELECT statements are allowed, got: " + normalized);
        }
        if (normalized.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Only a single SQL statement is allowed, got: " + normalized);
        }
        if (DANGEROUS_KEYWORDS.matcher(normalized).find()) {
            throw new IllegalArgumentException("Only SELECT statements are allowed, got: " + normalized);
        }

        logger.info("Executing generated SQL: {}", normalized);
        return queryExecutor.execute(normalized);
    }

    /**
     * Strips markdown code fences and any trailing semicolon that the model may add.
     */
    private static String normalize(String sql) {
        if (sql == null) {
            return "";
        }
        String result = sql.trim().toUpperCase();

        if (result.startsWith("```")) {
            result = result.substring(3).trim();
            if (result.regionMatches(true, 0, "sql", 0, 3)) {
                result = result.substring(3).trim();
            }
            int fenceEnd = result.lastIndexOf("```");
            if (fenceEnd >= 0) {
                result = result.substring(0, fenceEnd).trim();
            }
        }
        while (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

    public String getSchemaDescription() {
        return """
                Table: doctors
                Columns: id (INT), name (VARCHAR), specialization (VARCHAR), experience_years (INT), description (VARCHAR)
                
                Table: slots
                Columns: id (INT), doctor_id (INT, references doctors.id), slot_date (DATE), slot_time (TIME), is_available (BOOLEAN)
                """;
    }
}
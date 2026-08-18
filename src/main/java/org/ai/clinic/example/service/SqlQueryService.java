package org.ai.clinic.example.service;

import org.ai.clinic.example.repository.QueryExecutor;
import org.ai.clinic.example.repository.QueryExecutor.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Executes the read-only SQL produced by the model against the in-memory clinic database.
 *
 * <p>Because the SQL is model-generated, {@link #executeSelect(String)} acts as a guard rail:
 * it accepts a single {@code SELECT} statement only and rejects anything that could mutate data.
 *
 * <p>Validation runs against an upper-cased <em>copy</em> of the statement, while the statement
 * that actually reaches the database keeps its original casing — otherwise string literals such
 * as {@code LIKE '%smith%'} would be silently corrupted.
 */
@Service
public class SqlQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SqlQueryService.class);

    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "\\b(DROP|INSERT|UPDATE|DELETE|CREATE|ALTER|TRUNCATE|EXEC|EXECUTE|MERGE"
                    + "|CALL|SCRIPT|GRANT|REVOKE|SET|RUNSCRIPT|CSVWRITE|CSVREAD"
                    + "|FILE_READ|FILE_WRITE|LINK_SCHEMA|SHUTDOWN)\\b");

    private static final Set<String> ALLOWED_TABLES = Set.of("DOCTORS", "SLOTS");

    private final QueryExecutor queryExecutor;

    public SqlQueryService(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    /**
     * Runs a single {@code SELECT} statement and returns the rows.
     *
     * <p>The statement is cleaned up (markdown fences and trailing semicolons removed) but its
     * casing is preserved, so string literals stay intact.
     *
     * @throws IllegalArgumentException if the statement is blank, is not a {@code SELECT},
     *                                  or contains more than one statement
     */
    public QueryResult executeSelect(String sql) {
        String cleaned = clean(sql);

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("SQL statement must not be empty");
        }

        // Strip SQL comments before validation to prevent evasion via -- or /* */
        String noComments = stripComments(cleaned);
        String forChecks = noComments.toUpperCase(Locale.ROOT);

        if (!forChecks.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT statements are allowed");
        }
        if (noComments.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Only a single SQL statement is allowed");
        }
        if (DANGEROUS_KEYWORDS.matcher(forChecks).find()) {
            throw new IllegalArgumentException("Statement contains disallowed keywords");
        }
        validateAllowedTables(forChecks);

        logger.info("Executing generated SQL: {}", cleaned);
        return queryExecutor.execute(cleaned);
    }

    /**
     * Strips markdown code fences and any trailing semicolon that the model may add.
     * The casing of the statement is left untouched.
     */
    private static String clean(String sql) {
        if (sql == null) {
            return "";
        }
        String result = sql.trim();

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

    private static String stripComments(String sql) {
        // Remove block comments /* ... */
        String result = sql.replaceAll("/\\*.*?\\*/", " ");
        // Remove line comments -- ...
        result = result.replaceAll("--[^\n]*", " ");
        return result.trim();
    }

    private static void validateAllowedTables(String upperSql) {
        // Extract identifiers that follow FROM or JOIN keywords and verify they are allowed
        var tableMatcher = Pattern.compile("\\b(?:FROM|JOIN)\\s+([A-Z_][A-Z0-9_]*)\\b").matcher(upperSql);
        while (tableMatcher.find()) {
            String table = tableMatcher.group(1);
            if (!ALLOWED_TABLES.contains(table)) {
                throw new IllegalArgumentException("Query references disallowed table: " + table);
            }
        }
    }

    public String getSchemaDescription() {
        return """
                Table: doctors
                Columns: id (INT, PK), name (VARCHAR - full name e.g. "Dr. Emily Smith"),
                         specialization (VARCHAR - e.g. "Orthodontist", "Pediatric Dentist", "Oral Surgeon"),
                         experience_years (INT), description (VARCHAR - free text about the doctor)

                Table: slots
                Columns: id (INT, PK), doctor_id (INT, FK -> doctors.id),
                         slot_date (DATE - the calendar date of the appointment slot),
                         slot_time (TIME - start time of the slot e.g. '09:00:00'),
                         is_available (BOOLEAN - TRUE means the slot is free and can be booked, FALSE means already taken)

                Relationships: slots.doctor_id references doctors.id (many slots per doctor).
                To find available appointments, filter: is_available = TRUE.
                """;
    }
}
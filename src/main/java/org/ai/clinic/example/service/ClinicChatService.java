package org.ai.clinic.example.service;

import org.ai.clinic.example.dto.ChatCompletionRequest;
import org.ai.clinic.example.dto.ChatCompletionResponse;
import org.ai.clinic.example.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClinicChatService {

    private static final Logger logger = LoggerFactory.getLogger(ClinicChatService.class);

    private final AiProxyService aiProxyService;
    private final SqlQueryService sqlQueryService;
    private final PolicyService policyService;

    public ClinicChatService(AiProxyService aiProxyService,
                             SqlQueryService sqlQueryService,
                             PolicyService policyService) {
        this.aiProxyService = aiProxyService;
        this.sqlQueryService = sqlQueryService;
        this.policyService = policyService;
    }

    /**
     * Sends a single user message and returns the assistant answer as plain text.
     * Convenience wrapper around {@link #complete(ChatCompletionRequest)} for single-turn queries.
     */
    public String ask(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("query must not be empty");
        }
        ChatCompletionResponse response = complete(ChatCompletionRequest.ofUserMessage(query));
        String content = response.firstContent();
        if (content == null) {
            throw new IllegalStateException("AI proxy returned no choices for clinic question");
        }
        return content;
    }

    /**
     * Runs the full clinic RAG pipeline (SQL generation + execution + final answer) for a
     * full conversation history, and returns the raw chat completion response.
     *
     * <p>Supports multi-turn conversations: uses the whole history to understand follow-up
     * questions (e.g. "What about tomorrow?"), and generates SQL based on the latest
     * user message in context.
     */
    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        List<ChatMessage> history = request.messages();
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }

        String latestUserQuestion = extractLastUserMessage(history);
        String conversationText = formatConversation(history);

        // Step 1: generate SQL (taking full conversation context into account)
        String sql = generateSqlIfNeeded(conversationText);
        String dbResultText = "";

        if (!"NONE".equalsIgnoreCase(sql.trim())) {
            String result = tryExecuteWithRetry(sql, latestUserQuestion, 1);
            dbResultText = "SQL query executed (already filtered according to the latest user question):\n"
                    + sql + "\n\nResult:\n" + result;
        }

        // Step 2: final answer — system prompt with context + full message history
        String systemPrompt = """
                You are a helpful assistant for a dental clinic.
                Use the following clinic policy information and/or database results to answer the user's question.
                Be concise and friendly. Do not invent information that is not provided below.
                The database result (if present) is already filtered according to the latest user question,
                so all returned rows are relevant even if they don't repeat the filter criteria as columns.
                If the database result is empty or shows no rows, clearly state that no matching data was found.
                Take the full conversation history into account when answering follow-up questions.

                CLINIC POLICY:
                %s

                %s
                """.formatted(policyService.getPolicyText(), dbResultText);

        List<ChatMessage> fullMessages = new ArrayList<>();
        fullMessages.add(ChatMessage.system(systemPrompt));
        fullMessages.addAll(history);

        ChatCompletionRequest finalRequest = new ChatCompletionRequest(fullMessages, null, null);
        return aiProxyService.complete(finalRequest);
    }

    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        throw new IllegalArgumentException("No user message found in the conversation");
    }

    private String formatConversation(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            sb.append(m.role()).append(": ").append(m.content()).append("\n");
        }
        return sb.toString();
    }

    private String tryExecuteWithRetry(String sql, String userQuestion, int attemptsLeft) {
        try {
            List<Map<String, Object>> rows = sqlQueryService.executeSelect(sql);
            return formatRowsAsTable(rows);
        } catch (Exception e) {
            logger.warn("Generated SQL failed: {}. SQL was: {}", e.getMessage(), sql);
            if (attemptsLeft <= 0) {
                return "Database query failed, answer using available context only.";
            }
            String fixPrompt = """
                    The following SQL query failed on H2 database with error: %s

                    SQL:
                    %s

                    Fix the query for H2 syntax (use DATEADD instead of INTERVAL, no semicolon,
                    use CURRENT_DATE instead of CURDATE, use UPPER() for case-insensitive text matching).
                    Respond with ONLY the corrected SQL SELECT statement.
                    """.formatted(e.getMessage(), sql);

            ChatCompletionRequest request = new ChatCompletionRequest(
                    List.of(ChatMessage.user(fixPrompt)), 0.0, null);
            String fixedSql = aiProxyService.complete(request).firstContent();
            return tryExecuteWithRetry(fixedSql.trim(), userQuestion, attemptsLeft - 1);
        }
    }

    private String generateSqlIfNeeded(String conversationText) {
        String sqlPrompt = """
                You are a SQL generator for a dental clinic database running on H2 (H2 database, ANSI-ish SQL dialect).

                Database schema:
                %s

                IMPORTANT H2 SYNTAX RULES:
                - To add days to a date, use: DATEADD('DAY', 7, CURRENT_DATE)
                - Do NOT use MySQL syntax like INTERVAL 7 DAY
                - Do NOT end the query with a semicolon
                - Use CURRENT_DATE instead of CURDATE()
                - H2 string comparisons (including LIKE) are CASE-SENSITIVE by default.
                  Always wrap both sides in UPPER() for any text matching, for example:
                  UPPER(doctors.specialization) LIKE UPPER('%%orthodontist%%')
                  UPPER(doctors.name) LIKE UPPER('%%smith%%')

                IMPORTANT RESULT RULES:
                - Always include identifying columns in the SELECT list (e.g. doctor name, specialization)
                  even if they are already used in the WHERE clause, so the result is self-explanatory.

                Below is the full conversation history between a user and an assistant.
                Decide if answering the LATEST user message requires querying the database.
                Use earlier messages as context for follow-up questions (e.g. "What about tomorrow?").

                CONVERSATION HISTORY:
                %s

                - If a database query IS needed, respond with ONLY a valid SQL SELECT statement
                  (no explanation, no markdown, no semicolon at the end).
                - If NOT needed (e.g. general policy or doctor description questions), respond with exactly: NONE
                """.formatted(sqlQueryService.getSchemaDescription(), conversationText);

        ChatCompletionRequest request = new ChatCompletionRequest(
                List.of(ChatMessage.user(sqlPrompt)), 0.0, null);

        ChatCompletionResponse response = aiProxyService.complete(request);
        String content = response.firstContent();
        return content == null ? "NONE" : content.trim();
    }

    private static String formatRowsAsTable(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "(no results)";
        }
        List<String> columns = new ArrayList<>(rows.getFirst().keySet());
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", columns)).append('\n');
        sb.append("-".repeat(columns.size() * 15)).append('\n');
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(" | ");
                Object value = row.get(columns.get(i));
                sb.append(value == null ? "" : value.toString());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
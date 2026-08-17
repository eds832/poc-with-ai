# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 4.0.0 dental clinic assistant application (Java 25) with a RAG (Retrieval-Augmented Generation) pipeline. The app accepts patient questions via chat, generates SQL queries against an in-memory clinic database (H2), and returns grounded answers using an AI proxy (EPAM AI). Built with Maven.

## Build & Development Commands

### Build the project
```bash
mvn clean install
```

### Run the application
```bash
mvn spring-boot:run
```
The application starts on `http://localhost:8080`. A chat widget is available at the root page.

### Run tests
```bash
mvn test
```

### Run a single test
```bash
mvn test -Dtest=TestClassName
```

### Package as JAR
```bash
mvn package
```

## Architecture

### Application Flow (RAG Pipeline)

1. User sends a message via chat widget or API
2. `ClinicChatService` sends the conversation to the AI proxy to generate SQL (or `NONE` if no DB query needed)
3. If SQL is generated, `SqlQueryService` validates it (SELECT-only guard) and `QueryExecutor` executes it against H2
4. If SQL execution fails, one retry attempt is made (AI fixes the H2 syntax)
5. A final prompt is built with clinic policy + DB results + conversation history, sent to the AI proxy for the answer

### Layers

- **Main Application**: `DemoApplication.java` — Spring Boot entry point
- **Controller Layer**
  - `controller/ClinicChatController.java` — REST endpoints for the clinic chat (`/clinic/ask`, `/clinic/ask/text`, `/clinic/chat`)
  - `controller/DebugController.java` — debug endpoints to dump raw DB tables (`/debug/doctors`, `/debug/slots`)
  - `controller/GlobalExceptionHandler.java` — `@RestControllerAdvice` mapping `AiProxyException` → 502, `IllegalArgumentException` → 400
- **Service Layer**
  - `service/ClinicChatService.java` — RAG orchestration (SQL generation → execution → final answer)
  - `service/SqlQueryService.java` — SQL validation (SELECT-only, keyword blocklist with word-boundary matching) and normalization
  - `service/PolicyService.java` — loads `policy.txt` from classpath at startup
  - `service/AiProxyService.java` — calls the EPAM AI proxy `chat/completions` endpoint via `RestClient`
  - `service/AiProxyException.java` — exception for proxy failures
- **Repository Layer**
  - `repository/QueryExecutor.java` — executes raw SQL via `JdbcTemplate`, caps results at 200 rows
- **DTOs**: `dto/` — `ChatCompletionRequest`, `ChatCompletionResponse`, `ChatMessage`, `AskResponse`
- **Config**: `config/AiProxyProperties.java` (`ai-proxy.*` properties) and `config/AiProxyConfig.java` (dedicated `aiProxyRestClient` bean with timeouts + `Api-Key` header)
- **Resources**
  - `schema.sql` — H2 table definitions (doctors, slots)
  - `data.sql` — seed data (6 doctors, slots relative to CURRENT_DATE)
  - `policy.txt` — clinic policy text used as grounding context
  - `static/index.html` — marketing page with embedded chat widget

## AI Proxy Integration

Target URL: `{ai-proxy.base-url}/openai/deployments/{ai-proxy.deployment}/chat/completions`

Configuration from environment variables (`src/main/resources/application.properties`):

| Environment Variable | Property | Required | Default |
| --- | --- | --- | --- |
| `AI_PROXY_BASE_URL` | `ai-proxy.base-url` | Yes | (none — app fails fast if not set) |
| `AI_PROXY_DEPLOYMENT` | `ai-proxy.deployment` | No | `anthropic.claude-v3-haiku` |
| `AI_PROXY_API_KEY` | `ai-proxy.api-key` | No | (empty) |
| `AI_PROXY_TIMEOUT` | `ai-proxy.timeout` | No | `60s` |

Set required variables before running:

```bash
$env:AI_PROXY_BASE_URL="https://your-proxy-host"
$env:AI_PROXY_API_KEY="your-api-key"
```

## Endpoints

```bash
# JSON response: { "query": ..., "answer": ..., "model": ... }
curl "http://localhost:8080/clinic/ask?query=When+is+Dr.+Smith+available?"

# Plain-text answer only
curl "http://localhost:8080/clinic/ask/text?query=When+is+Dr.+Smith+available?"

# Full multi-turn chat (what the UI widget uses)
curl -X POST "http://localhost:8080/clinic/chat" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"Who is available tomorrow?"}]}'

# Debug: dump all doctors / slots
curl "http://localhost:8080/debug/doctors"
curl "http://localhost:8080/debug/slots"
```

## Database (H2 In-Memory)

- Schema is created by `schema.sql` on every startup (`spring.sql.init.mode=always`)
- Seed data in `data.sql` uses `DATEADD('DAY', N, CURRENT_DATE)` for relative slot dates
- `SET IGNORECASE TRUE` is set for case-insensitive identifier matching
- No JPA/Hibernate — all access is raw JDBC via `JdbcTemplate`

## Key Dependencies

- Spring Boot 4.0.0 (parent POM)
- Spring Boot Starter WebMvc
- `spring-boot-restclient` — provides `RestClient.Builder` auto-configuration
- `spring-boot-starter-jdbc` — `JdbcTemplate` and datasource auto-configuration
- `spring-boot-starter-validation` — bean validation for `@ConfigurationProperties`
- H2 Database (runtime, in-memory)
- Lombok (compile-time)
- Spring Boot Starter Test + `spring-boot-starter-webmvc-test` (test scope)

## Security
- API keys must NEVER be hardcoded in project files — always use environment variables.
- `SqlQueryService` enforces SELECT-only queries with word-boundary keyword matching to prevent data mutation from AI-generated SQL.

## Testing
- Always create unit tests that cover all branches and edge cases.
- Use `@WebMvcTest` slice tests for controllers (import from `org.springframework.boot.webmvc.test.autoconfigure` in Boot 4).
- Use `@MockitoBean` (not the deprecated `@MockBean`) for test mocks in Boot 4.
- Use `EmbeddedDatabaseBuilder` with `schema.sql`/`data.sql` for repository integration tests.

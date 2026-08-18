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
2. `ClinicChatService` validates roles, formats conversation as XML-delimited messages, and sends to the AI proxy to generate SQL (or `NONE` if no DB query needed)
3. If SQL is generated, `SqlQueryService` strips comments, validates it (SELECT-only guard + table whitelist) and `QueryExecutor` executes it against H2 with query timeout and row cap
4. If SQL execution fails, one retry attempt is made (AI fixes the H2 syntax)
5. A final prompt is built with clinic policy + DB results + conversation history, sent to the AI proxy for the answer

### Layers

- **Main Application**: `DemoApplication.java` — Spring Boot entry point
- **Controller Layer**
  - `controller/ClinicChatController.java` — REST endpoints for the clinic chat (`/clinic/ask`, `/clinic/ask/text`, `/clinic/chat`) with `@Valid` request body validation
  - `controller/DebugController.java` — debug endpoints to dump raw DB tables (`/debug/doctors`, `/debug/slots`), gated by `@Profile("dev")`
  - `controller/GlobalExceptionHandler.java` — `@RestControllerAdvice` mapping `AiProxyException` → 502, `IllegalArgumentException` → 400, `MethodArgumentNotValidException` → 400
- **Service Layer**
  - `service/ClinicChatService.java` — RAG orchestration (role validation, XML-delimited formatting, SQL generation with `max_tokens=200` → execution → final answer)
  - `service/SqlQueryService.java` — SQL validation (comment stripping, SELECT-only keyword blocklist with word-boundary matching, table whitelist: DOCTORS/SLOTS only)
  - `service/PolicyService.java` — loads `policy.txt` + doctor info from DB at startup
  - `service/AiProxyService.java` — calls the EPAM AI proxy `chat/completions` endpoint via `RestClient`
  - `service/AiProxyException.java` — exception for proxy failures
- **Repository Layer**
  - `repository/QueryExecutor.java` — executes raw SQL via `JdbcTemplate`, caps results at 200 rows (`setMaxRows`), enforces 10s query timeout (`setQueryTimeout`), returns `QueryResult` record
- **DTOs**: `dto/` — `ChatCompletionRequest`, `ChatCompletionResponse`, `ChatMessage` (with `@NotBlank`, `@Size`, `@Pattern` validation), `AskResponse`
- **Config**
  - `config/AiProxyProperties.java` — `ai-proxy.*` properties with `@Validated` + `@Pattern` on baseUrl
  - `config/AiProxyConfig.java` — dedicated `aiProxyRestClient` bean with timeouts + `Api-Key` header
  - `config/WebConfig.java` — CORS configuration for `/clinic/**` (GET/POST)
  - `config/RateLimitFilter.java` — IP-based rate limiter (20 req/min per IP on `/clinic/*`)
- **Resources**
  - `schema.sql` — H2 table definitions (doctors, slots)
  - `data.sql` — seed data (6 doctors, slots relative to CURRENT_DATE)
  - `policy.txt` — clinic policy text (hours, cancellation, booking rules)
  - `static/index.html` — marketing page with embedded chat widget (client-side hardening: message cap, request timeout, error hiding, history rollback)

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

# Health check (Spring Boot Actuator)
curl "http://localhost:8080/actuator/health"

# Debug: dump all doctors / slots (only active with spring.profiles.active=dev)
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
- Spring Boot Starter Actuator — health/info endpoints
- `spring-boot-restclient` — provides `RestClient.Builder` auto-configuration
- `spring-boot-starter-jdbc` — `JdbcTemplate` and datasource auto-configuration
- `spring-boot-starter-validation` — bean validation for DTOs and `@ConfigurationProperties`
- H2 Database (runtime, in-memory)
- Spring Boot Starter Test + `spring-boot-starter-webmvc-test` (test scope)

## Security

- API keys must NEVER be hardcoded in project files — always use environment variables.
- `SqlQueryService` enforces SELECT-only queries with word-boundary keyword matching (includes H2-specific threats: `CALL`, `SCRIPT`, `RUNSCRIPT`, `CSVWRITE`, `FILE_READ`, `SHUTDOWN`, etc.).
- Table whitelist: only `DOCTORS` and `SLOTS` tables are queryable.
- SQL comments (`--`, `/* */`) are stripped before validation to prevent bypass.
- Prompt injection defense: conversation messages are XML-delimited and marked as DATA; roles are validated against an allowlist.
- `max_tokens=200` on SQL generation calls to prevent token waste.
- Input validation via Jakarta Bean Validation on all incoming DTOs (`@NotBlank`, `@Size(max=4000)`, `@Pattern` for roles, `@Size(max=50)` on message lists).
- Rate limiting: 20 requests/minute per IP on `/clinic/*`.
- CORS: only `GET`/`POST` allowed on `/clinic/**`.
- Debug endpoints gated behind `@Profile("dev")`.
- User queries logged at DEBUG level only (PII protection).
- Query timeout (10s) and row cap (200) on DB execution.

## Testing

- Always create unit tests that cover all branches and edge cases.
- Use `@WebMvcTest` slice tests for controllers (import from `org.springframework.boot.webmvc.test.autoconfigure` in Boot 4).
- Use `@MockitoBean` (not the deprecated `@MockBean`) for test mocks in Boot 4.
- Use `EmbeddedDatabaseBuilder` with `schema.sql`/`data.sql` for repository integration tests.
- 49 tests across 7 test classes covering controllers, services, and repository.

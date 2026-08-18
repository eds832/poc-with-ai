# BrightSmile Dental Clinic AI Assistant

A Spring Boot 4 demo application that implements a RAG (Retrieval-Augmented Generation) pipeline for a dental clinic chatbot. Patients can ask questions about doctor availability, specializations, and clinic policies through a web chat interface.

## How It Works

1. Patient sends a question via the chat widget (or REST API)
2. The AI model decides whether a database query is needed and generates SQL
3. The SQL is validated (SELECT-only, table whitelist, comment stripping) and executed against an in-memory H2 database
4. A final answer is generated using the DB results + clinic policy as grounding context

This ensures the chatbot only answers based on real data — it cannot hallucinate appointment slots or doctor details.

## Quick Start

### Prerequisites
- Java 25+
- Maven 3.8+
- An AI proxy endpoint (EPAM AI Dial or compatible OpenAI-format API)

### Run

```bash
# Set required environment variables
export AI_PROXY_BASE_URL="https://your-proxy-host"
export AI_PROXY_API_KEY="your-api-key"

# Build and run
mvn spring-boot:run
```

Open http://localhost:8080 — click the chat bubble in the bottom-right corner.

### PowerShell

```powershell
$env:AI_PROXY_BASE_URL="https://your-proxy-host"
$env:AI_PROXY_API_KEY="your-api-key"
mvn spring-boot:run
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/clinic/ask?query=...` | Single-turn question, JSON response |
| GET | `/clinic/ask/text?query=...` | Single-turn question, plain text |
| POST | `/clinic/chat` | Multi-turn conversation (used by the UI) |
| GET | `/actuator/health` | Health check |

### Debug Endpoints

The following endpoints are only available when the application is running with the `dev` profile:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/debug/doctors` | Dump all doctors as JSON |
| GET | `/debug/slots` | Dump all appointment slots as JSON |

To enable debug mode, start the application with the `dev` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or via environment variable:

```bash
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
```

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `AI_PROXY_BASE_URL` | *(required)* | Base URL of the AI proxy |
| `AI_PROXY_API_KEY` | *(empty)* | API key for the proxy |
| `AI_PROXY_DEPLOYMENT` | `anthropic.claude-v3-haiku` | Model deployment name |
| `AI_PROXY_TIMEOUT` | `60s` | Request timeout |

## Security

- **SQL injection defense**: SELECT-only guard with extended H2-specific keyword blocklist, table whitelist (DOCTORS/SLOTS only), comment stripping
- **Prompt injection defense**: XML-delimited conversation messages, role validation, data-only markers
- **Input validation**: Jakarta Bean Validation on all DTOs (message length cap at 4000 chars, max 50 messages per request)
- **Rate limiting**: 20 requests/minute per IP on clinic endpoints
- **CORS**: restricted to GET/POST on `/clinic/**`
- **PII protection**: user queries logged at DEBUG level only
- **DB safety**: 10s query timeout, 200-row cap, `max_tokens=200` on SQL generation
- **Environment gating**: debug endpoints require `dev` profile

## Tech Stack

- **Java 25** / **Spring Boot 4.0.0**
- **H2** in-memory database (schema + seed data loaded on startup)
- **RestClient** for AI proxy communication
- **Spring Boot Actuator** for health/info endpoints
- **Jakarta Bean Validation** for input validation
- **Maven** build tool
- **JUnit 5 + Mockito** for testing (49 tests)

## Running Tests

```bash
mvn test
```

## Code Coverage (JaCoCo)

JaCoCo runs automatically during `mvn test`. To generate and view the coverage report:

```bash
mvn test
```

The HTML report is generated at `target/site/jacoco/index.html`. Open it in a browser to inspect line and branch coverage per class.

To generate only the report (if tests were already run):

```bash
mvn jacoco:report
```

## Linting (Checkstyle)

The project uses [Checkstyle](https://checkstyle.org/) with Google's Java style rules. To run the linter:

```bash
mvn checkstyle:check
```

This will print violations to the console and fail if errors are found. To generate an HTML report without failing:

```bash
mvn checkstyle:checkstyle
```

The report is generated at `target/site/checkstyle.html`.

## Project Structure

```
src/main/java/org/ai/clinic/example/
├── DemoApplication.java
├── config/          # AI proxy config, CORS, rate limiting
├── controller/      # REST endpoints + global exception handler
├── dto/             # Request/response records with validation
├── repository/      # Raw JDBC query executor (QueryResult record)
└── service/         # Business logic (RAG pipeline, SQL validation, policy)

src/main/resources/
├── schema.sql       # Table definitions
├── data.sql         # Seed data (6 doctors, appointment slots)
├── policy.txt       # Clinic policy text for grounding
└── static/index.html  # Chat UI with client-side hardening
```

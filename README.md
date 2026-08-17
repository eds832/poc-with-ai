# BrightSmile Dental Clinic AI Assistant

A Spring Boot 4 demo application that implements a RAG (Retrieval-Augmented Generation) pipeline for a dental clinic chatbot. Patients can ask questions about doctor availability, specializations, and clinic policies through a web chat interface.

## How It Works

1. Patient sends a question via the chat widget (or REST API)
2. The AI model decides whether a database query is needed and generates SQL
3. The SQL is validated (SELECT-only) and executed against an in-memory H2 database
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
| GET | `/debug/doctors` | Dump all doctors (debug) |
| GET | `/debug/slots` | Dump all slots (debug) |

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `AI_PROXY_BASE_URL` | *(required)* | Base URL of the AI proxy |
| `AI_PROXY_API_KEY` | *(empty)* | API key for the proxy |
| `AI_PROXY_DEPLOYMENT` | `anthropic.claude-v3-haiku` | Model deployment name |
| `AI_PROXY_TIMEOUT` | `60s` | Request timeout |

## Tech Stack

- **Java 25** / **Spring Boot 4.0.0**
- **H2** in-memory database (schema + seed data loaded on startup)
- **RestClient** for AI proxy communication
- **Maven** build tool
- **JUnit 5 + Mockito** for testing

## Running Tests

```bash
mvn test
```

## Project Structure

```
src/main/java/org/ai/clinic/example/
├── DemoApplication.java
├── config/          # AI proxy configuration
├── controller/      # REST endpoints
├── dto/             # Request/response records
├── repository/      # Raw JDBC query executor
└── service/         # Business logic (RAG pipeline, SQL validation, policy)

src/main/resources/
├── schema.sql       # Table definitions
├── data.sql         # Seed data (6 doctors, appointment slots)
├── policy.txt       # Clinic policy text for grounding
└── static/index.html  # Chat UI
```

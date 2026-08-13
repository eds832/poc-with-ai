# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 4.0.0 demo application (Java 25) with a simple REST controller. The project demonstrates a basic web MVC setup with Maven as the build tool.

## Build & Development Commands

### Build the project
```bash
mvn clean install
```

### Run the application
```bash
mvn spring-boot:run
```
The application starts on the default Spring Boot port (typically `http://localhost:8080`).

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

- **Main Application**: `src/main/java/org/ai/clinic/example/DemoApplication.java` — Spring Boot entry point with `@SpringBootApplication` annotation
- **Controller Layer**
  - `controller/MainController.java` — REST controller with root mapping (`@RestController` at `/`), returns `"done"`
  - `controller/AiController.java` — endpoints that forward a query to the AI proxy
  - `controller/GlobalExceptionHandler.java` — `@RestControllerAdvice` mapping `AiProxyException` → 502, `IllegalArgumentException` → 400
- **Service Layer**: `service/AiProxyService.java` — calls the EPAM AI proxy `chat/completions` endpoint via `RestClient`
- **DTOs**: `dto/` — `ChatCompletionRequest`, `ChatCompletionResponse`, `ChatMessage`, `AskResponse`
- **Config**: `config/AiProxyProperties.java` (`ai-proxy.*` properties) and `config/AiProxyConfig.java` (dedicated `aiProxyRestClient` bean with timeouts + `Api-Key` header)

## AI Proxy Integration

Target URL: `{ai-proxy.base-url}/openai/deployments/{ai-proxy.deployment}/chat/completions`

Configuration from environment variables (`src/main/resources/application.properties`):

| Environment Variable | Property | Required | Default |
| --- | --- | --- | --- |
| `AI_PROXY_BASE_URL` | `ai-proxy.base-url` | Yes | (none — app fails fast if not set) |
| `AI_PROXY_DEPLOYMENT` | `ai-proxy.deployment` | No | `anthropic.claude-opus-5` |
| `AI_PROXY_API_KEY` | `ai-proxy.api-key` | No | (empty) |
| `AI_PROXY_TIMEOUT` | `ai-proxy.timeout` | No | `60s` |

Set required variables before running:

```bash
$env:AI_PROXY_BASE_URL="https://your-proxy-host"
$env:AI_PROXY_API_KEY="your-api-key"
```

### Endpoints

```bash
# JSON response: { "query": ..., "answer": ..., "model": ... }
curl "http://localhost:8080/ai/ask?query=My%20content"

# plain-text answer only
curl "http://localhost:8080/ai/ask/text?query=My%20content"

# full passthrough of a messages payload
curl -X POST "http://localhost:8080/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"My content"}]}'
```

## Key Dependencies

- Spring Boot 4.0.0 (parent POM)
- Spring Boot Starter WebMvc
- `spring-boot-restclient` — provides `RestClient.Builder` auto-configuration and `ClientHttpRequestFactoryBuilder` / `HttpClientSettings` (note: in Boot 4 `ClientHttpRequestFactorySettings` was replaced by `HttpClientSettings`)
- Spring Boot Starter Test (test scope)
- `spring-boot-starter-webmvc-test` (test scope) — required for `@WebMvcTest`

## Security
- always make sure that API keys and real URL are not exposed in the project files.

## Testing
- always create unit tests that cover all branches and edge cases.
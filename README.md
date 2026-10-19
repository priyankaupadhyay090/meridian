# Meridian

Meridian is a Spring Boot and React app that helps German online shops manage their couriers.

- Compare courier services and manage delivery rules
- Check courier contracts against relevant German freight and postal law
- Change routing safely: every change needs a test run and a second person's approval
- Interpret contract clauses with a local AI language model (Ollama), while deterministic checks stay in control

![Meridian homepage showing courier comparisons](meridian-home.png)

## How to run

Requires Java 21, Node 24 and Docker.

1. Start the supporting services (S3 via LocalStack, Kafka, Prometheus, Grafana):

   ```bash
   docker compose up -d
   ```

2. Start the backend:

   ```bash
   ./gradlew bootRun
   ```

3. Start the web app:

   ```bash
   cd web && npm ci && npm run dev
   ```

Open http://localhost:5180.

The AI clause interpretation expects Ollama at `http://localhost:11434` (override with `OLLAMA_URL`). Kafka is read from `KAFKA_BOOTSTRAP` (default `localhost:9092`).

## Tests

```bash
./gradlew test          # backend
cd web && npm test      # web unit tests
cd web && npm run e2e   # Playwright end-to-end tests
```

## Architecture

- The backend is split by feature (`routing`, `contract`, `compliance`, `change`, `delivery`, and others) under `src/main/java/com/meridian/`.
- Each feature follows a layered architecture:
  - `domain`: business rules and models
  - `application`: use cases
  - `adapter`: web, Kafka, storage and LLM integrations
- The web app lives in `web/`.

## Local services

Defined in `compose.yaml`:

| Service | URL |
| --- | --- |
| Kafka UI | http://localhost:8090 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (anonymous viewer; admin password from `GRAFANA_PASSWORD`, default `meridian`) |
| LocalStack S3 | http://localhost:4566 |

Prometheus and Grafana configuration is in `monitoring/`.

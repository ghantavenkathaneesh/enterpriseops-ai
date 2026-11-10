# Testing report

Honest status as of the last build. "Executed" means it was actually run in the build sandbox, which had Java 21 (JRE only), Node, Python 3.12 and scikit-learn, but no Maven, javac, Docker, PostgreSQL, FastAPI, pytest or internet.

## Executed, with results

| Check | Command | Result |
|---|---|---|
| ML model tests (5) | `python ai-service/tests/test_models.py` | 5 passed |
| Simulator tests (3) | `python simulator/test_simulate.py` | 3 passed |
| Database structure (translated to SQLite) | `python database/validate_sqlite.py` | 12 tables created; dedupe index, priority CHECK, metric range CHECK and foreign key all rejected bad data as intended |
| Python syntax | `python -m py_compile` on all Python files | OK |
| Model training and evaluation | `python scripts/train.py` | Classifier acc 0.96, anomaly F1 0.74 (synthetic data, see AI_ML.md) |
| Frontend syntax and imports | esbuild bundle of `src/main.tsx` with npm packages external | OK |
| Playwright spec syntax | esbuild transform of `e2e/*.ts` | OK |
| Config files | YAML and JSON parsers on compose, CI workflow, Postman collection | Valid |
| Java structure | brace/paren balance and package-path check on all 73 files | No problems (this is NOT a compile) |

## Written but NOT executed

- JUnit 5 and Mockito unit tests: `JwtServiceTest`, `TicketStatusTest`, `IncidentStatusTest`, `AnalyticsServiceTest`, `TicketServiceTest`, `MonitoringServiceTest` (`mvn test`).
- Spring Boot + Testcontainers integration tests: `ApiIntegrationIT` covers 401 without token, bad login, employee ticket creation and analytics 403, agent vs admin audit access, and metric ingestion creating only one incident (`mvn verify`, needs Docker).
- FastAPI endpoint tests: `ai-service/tests/test_api.py` (`pytest`).
- Playwright UI tests: login redirect, wrong password, logout, role-aware navigation, employee-creates / agent-assigns ticket, incident creation (`npx playwright test`).
- Postman collection with 13 requests including negative cases (401, 400).
- Docker Compose startup, health checks, Flyway migrations on PostgreSQL, GitHub Actions workflow.

## Known failures

None observed in what was executed. The Java and TypeScript code has never been compiled, so expect to fix some compile or type errors on first build. That is normal, and the CI workflow is set up to surface them.

## How to run everything on a full machine

```
cd ai-service && pip install -r requirements.txt && python scripts/generate_data.py && python scripts/train.py && pytest
cd ../backend && mvn verify
cd ../frontend && npm install && npm run build
cd .. && cp .env.example .env   # fill in the three secrets
docker compose up --build
cd frontend && DEMO_PASSWORD=<same as .env> npx playwright test
```

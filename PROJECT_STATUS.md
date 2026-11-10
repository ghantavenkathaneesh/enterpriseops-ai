# Project status

Last updated at the end of this build session. Nothing below is marked "tested" unless it was actually run.

## Checklist

| Component | Written | Executed / verified | Notes |
|---|---|---|---|
| Synthetic data generator | yes | **yes** | 1,500 tickets, 3,000 metric rows |
| Ticket classifier + anomaly detector | yes | **yes** | trained, saved, evaluated (synthetic, see docs/AI_ML.md) |
| Model tests (5) | yes | **yes, pass** | |
| FastAPI service (5 endpoints) | yes | no | FastAPI not installable offline; `py_compile` OK |
| FastAPI endpoint tests | yes | no | needs fastapi, httpx, pytest |
| PostgreSQL schema + migrations (12 tables) | yes | **partly** | structure checked via SQLite translation only; never run on PostgreSQL |
| SQL reporting queries | yes | no | PostgreSQL-specific (`FILTER`, `date_trunc`, `EXTRACT`) |
| Java backend (73 files: auth, users, tickets, incidents, assets, monitoring, analytics, notifications, audit, AI client, SSE, demo seeder) | yes | **no, never compiled** | no `javac`/Maven; only brace-balance and package checks |
| JUnit / Mockito / Testcontainers tests | yes | no | |
| React + TypeScript frontend (11 pages) | yes | **partly** | esbuild bundle OK (syntax + imports); no type-check, never rendered |
| Playwright tests (3 specs) | yes | no | |
| Metric simulator | yes | **yes** (logic) | generator tests pass; not run against a live API |
| Postman collection (13 requests) | yes | no | valid JSON |
| Dockerfiles x3, docker-compose.yml, .env.example | yes | no | YAML valid; Docker not available |
| GitHub Actions CI | yes | no | YAML valid |
| Documentation (README, architecture, DB + ER, API, AI/ML, testing, interview prep) | yes | n/a | ER and architecture diagrams are Mermaid source (PNG render failed in the sandbox) |
| Screenshots | **no** | no | UI never run in a browser |
| Git repository | initialised | no commits | no git identity configured; no remote, nothing pushed |

## Not implemented
Screenshots; rendered diagram images; audit-log page in the UI; UI to link an incident to a ticket; password change; refresh tokens; login rate limiting; email notifications; Redis (no real need yet); Kafka and Kubernetes (optional future work).

## Blocked by the build environment
Compiling and testing Java (no Maven/javac), running PostgreSQL and Flyway, installing FastAPI/pytest/npm packages (no internet), Docker, browsers for Playwright and screenshots, pushing to GitHub (no repository access given).

## Known risks to check first on a real machine
1. Java compile errors are likely somewhere in 73 untested files. Run `mvn -B verify` and fix top-down.
2. TypeScript errors are possible. Run `npm install && npm run typecheck`.
3. SSE with stateless JWT security: if the live stream returns 401 or 403 after connecting, the async dispatch needs an extra security rule.
4. Flyway or JPA mismatches (column type or name) would show at startup; `ddl-auto` is `none`, so mismatches surface as SQL errors at runtime, not at boot.
5. Isolation Forest precision is only 0.59 on synthetic data (expected false alarms).

## Exact commands on a fully equipped computer
```
cp .env.example .env     # fill POSTGRES_PASSWORD, JWT_SECRET, DEMO_PASSWORD
(cd ai-service && pip install -r requirements.txt && python scripts/generate_data.py && python scripts/train.py && pytest)
(cd backend && mvn -B verify)
(cd frontend && npm install && npm run build)
docker compose up --build
docker compose --profile simulator up -d simulator
(cd frontend && DEMO_PASSWORD=<value from .env> npx playwright test)
```

# Project status

Last updated 2026-11-10. Verification is recorded only when a command or workflow actually completed.

## Verified

| Component | Result | Evidence |
|---|---|---|
| Synthetic data generator and model training | Pass | Generated 1,500 synthetic tickets and 3,000 metric rows; classifier accuracy 0.96 and anomaly detector recall 1.0 on synthetic data. |
| AI service tests | Pass | `pytest -q`: 12 passed locally on Python 3.13; CI also passed on Python 3.12. |
| Database schema and simulator logic | Pass | SQLite migration checks and all 3 simulator logic checks passed locally and in CI. |
| Java backend and PostgreSQL integration tests | Pass | `mvn -B verify` passed in GitHub Actions on Java 21, including Testcontainers with PostgreSQL. |
| React and TypeScript production build | Pass | `npm run build` passed locally and in CI. Page-level code splitting keeps the initial bundle below the Vite warning threshold. |
| Dependency audit | Pass | `npm audit`: 0 vulnerabilities after updating Vite and React Router. |
| GitHub CI | Pass | All five jobs passed on the latest code, including the full-stack browser job. |
| Full-stack browser E2E | Pass | All 7 Playwright tests passed in Chromium against the Docker Compose stack, covering login, role permissions, tickets, incidents, AI classification, and live monitoring. |

## Not implemented

Screenshots and rendered diagram images; audit-log page in the UI; UI to link an incident to a ticket; password change; refresh tokens; login rate limiting; email notifications; Redis, Kafka and Kubernetes (optional future work).

## Remaining limits

- The full Compose stack cannot run locally in this Windows workspace because Docker and PostgreSQL are not installed. GitHub Actions successfully verified the stack in its Docker-enabled runner.
- The Java backend, frontend, SQL validation, simulator, AI service, PostgreSQL integration test, and browser E2E suite all pass in CI.
- The model metrics are from synthetic/template-derived data and should not be presented as real-world performance. The anomaly detector's precision is 0.59 and false alerts are expected.

## Commands

```sh
(cd ai-service && python scripts/generate_data.py && python scripts/train.py && pytest -q)
python database/validate_sqlite.py
python simulator/test_simulate.py
(cd backend && mvn -B verify)
(cd frontend && npm ci && npm run build)
docker compose --profile simulator up --build --detach --wait
(cd frontend && DEMO_PASSWORD=<value from .env> npx playwright test)
```

# EnterpriseOps AI

An IT operations platform that combines a help desk, incident management, asset tracking, simulated infrastructure monitoring with anomaly detection, and SQL-based analytics, with role-based access for admins, managers, support agents and employees.

> **Project status: work in progress.** The Python AI service, the metric simulator and the database design have been run and tested. The Java backend and the React frontend are fully written but **have not been compiled or run yet**. Read [PROJECT_STATUS.md](PROJECT_STATUS.md) for the exact breakdown. **All data in this project is synthetic.**

## Features

- **Authentication and access control:** JWT login, BCrypt password hashes, four roles, employees see only their own tickets
- **Tickets:** create, search and filter, assign, comment, validated status transitions, SLA due dates and breach flags
- **Incidents:** manual or auto-created from monitoring, severity, assignment, resolution notes
- **Assets and monitoring:** asset registry, CPU / RAM / disk / network readings, threshold and ML anomaly detection, duplicate-alert protection, live updates over server-sent events
- **AI service:** ticket category classifier (TF-IDF + Logistic Regression) and anomaly detector (Isolation Forest); the system keeps working if the AI service is down
- **Analytics:** tickets by status, priority and category, daily and monthly trends, average resolution time, SLA compliance, agent workload, incident severity, asset health, AI statistics, CSV export (all from SQL)
- **Notifications and audit log**

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, JWT, Flyway, Swagger/OpenAPI |
| Database | PostgreSQL 16 |
| AI service | Python 3.12, FastAPI, scikit-learn, pandas, joblib |
| Frontend | React, TypeScript, Vite, Axios, Recharts |
| Testing | JUnit 5, Mockito, Testcontainers, pytest, Playwright, Postman |
| DevOps | Docker, Docker Compose, GitHub Actions |

## Run it (needs Docker)

```
cp .env.example .env       # fill in POSTGRES_PASSWORD, JWT_SECRET (openssl rand -base64 48), DEMO_PASSWORD
docker compose up --build
docker compose --profile simulator up -d simulator     # optional: live synthetic metrics
```
- App: http://localhost:5173 · API docs: http://localhost:8080/swagger-ui.html
- Demo accounts (password is your `DEMO_PASSWORD`): `admin@`, `manager@`, `agent1@`, `agent2@`, `employee@` followed by `enterpriseops.local`

## Run without Docker
```
# 1. AI service (data and models are generated, not stored in git)
cd ai-service && pip install -r requirements.txt && python scripts/generate_data.py && python scripts/train.py && uvicorn app.main:app --port 8000
# 2. Database: create a PostgreSQL database "enterpriseops"; Flyway applies database/migrations automatically
# 3. Backend (Java 21 + Maven)
cd backend && DB_PASSWORD=... JWT_SECRET=... DEMO_PASSWORD=... SEED_DEMO=true mvn spring-boot:run
# 4. Frontend
cd frontend && npm install && npm run dev
```

## Documentation

[Architecture](docs/ARCHITECTURE.md) · [Database and ER diagram](docs/DATABASE.md) · [API](docs/API.md) · [AI/ML methodology](docs/AI_ML.md) · [Testing report](docs/TESTING.md) · [Interview prep](docs/INTERVIEW_PREP.md) · [Postman collection](postman/)

## Repository layout
```
ai-service/   FastAPI + scikit-learn (data generation, training, evaluation, tests)
backend/      Spring Boot API (feature packages, unit and integration tests)
frontend/     React + TypeScript + Vite dashboard, Playwright tests
database/     Flyway migrations, reporting SQL, validation script
simulator/    synthetic metric generator
docs/         architecture, database, API, AI/ML, testing, interview prep
```

## Honest limitations

- Models are trained on synthetic data; reported scores are not real-world performance (see [docs/AI_ML.md](docs/AI_ML.md))
- Live stream authenticates with a query-string token; the JWT is kept in `localStorage`
- No refresh tokens, login rate limiting, password change, email notifications or audit-log page yet
- No screenshots yet
- Possible future work: Redis caching, Kafka ingestion, Kubernetes

# Interview preparation

Only talk about what you have run and understood. Run the project locally first, fix compile errors, then use this.

## 30-second pitch
"EnterpriseOps AI is a full-stack IT operations platform I built: a Spring Boot REST backend with JWT security and PostgreSQL, a React and TypeScript dashboard, and a separate Python FastAPI service with two scikit-learn models. It handles tickets, incidents and assets, ingests simulated server metrics, flags anomalies, opens incidents without duplicates, and shows analytics computed from SQL. All data is synthetic."

## Questions to expect, with honest answers

**Why two backends (Java and Python)?** Spring Boot is a good fit for the business logic, security and transactions. Python has the ML libraries. They talk over HTTP, so either can be replaced.

**What happens if the AI service is down?** `AiClient` has a 3-second timeout and returns an empty result. Tickets get category "other"; monitoring still raises alerts from threshold rules.

**How do you avoid duplicate incidents?** The service checks for an active monitoring incident on that asset first, and a partial unique index in PostgreSQL enforces the same rule even under concurrent requests.

**How is security handled?** BCrypt hashes, signed JWT with expiry, role checks with `@PreAuthorize`, employees can only read their own tickets, validation on every request body, audit log restricted to admin and manager.

**Why Flyway?** Versioned, repeatable schema changes. Hibernate is set to `none` so the SQL files are the single source of truth.

**How good are your models?** On synthetic data the classifier scores 0.96 accuracy, but I know that is inflated because the text comes from templates. The anomaly detector has recall 1.0 and precision 0.59 on injected anomalies. For real use I would need real labelled data. (This answer shows you understand evaluation. Keep it.)

**Why SSE and not WebSockets?** Updates only go server to browser, and SSE is simpler and reconnects automatically.

**SQL questions it prepares you for:** group by with filters, `COUNT(*) FILTER`, date truncation for trends, joins for agent workload, partial indexes, why indexes on foreign keys and dates.

## Known weaknesses (say them before they ask)

- Token is kept in localStorage (XSS exposure) and passed in the query string for the SSE stream. A production system would use short-lived tokens, httpOnly cookies or a ticket exchange for SSE.
- No rate limiting on login, no refresh tokens, no password change endpoint.
- List endpoints do extra queries for related users (N+1); fine at this size.
- Everything runs on synthetic data.

## Resume wording

Use only the lines that are true after you have run the system.

- *Always true once you have run the ML part:* "Trained and evaluated a TF-IDF + Logistic Regression ticket classifier and an Isolation Forest anomaly detector (scikit-learn) on synthetic data; documented evaluation limits."
- *After the backend builds and the tests pass:* "Built a Spring Boot 3 / Java 21 REST API with JWT authentication, role-based access control, Flyway migrations on PostgreSQL, and JUnit/Mockito tests."
- *After the full stack runs with Docker Compose:* "Developed a React/TypeScript dashboard with live metric streaming (SSE), SQL-based analytics and CSV export; containerised with Docker Compose and GitHub Actions CI."
- Never put numbers on the resume that you did not measure yourself.

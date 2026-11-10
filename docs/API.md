# API reference

Base path `/api/v1`. Interactive docs when the backend is running: http://localhost:8080/swagger-ui.html. Send `Authorization: Bearer <token>` on every request except login.

| Area | Endpoint | Who |
|---|---|---|
| Auth | `POST /auth/login`, `POST /auth/logout`, `GET/PUT /auth/me` | anyone / logged-in |
| Users | `GET /users` (search, role, active), `GET /users/staff` | admin, manager / staff |
| | `POST /users`, `PUT /users/{id}`, `DELETE /users/{id}` (deactivates) | admin |
| Tickets | `POST /tickets`, `GET /tickets` (status, priority, category, assigneeId, q, page, size), `GET /tickets/{id}` | all (employees see only their own) |
| | `PUT /tickets/{id}`, `PATCH /tickets/{id}/assign`, `PATCH /tickets/{id}/status` | staff |
| | `GET/POST /tickets/{id}/comments` | all with access |
| Incidents | `POST/GET /incidents`, `GET /incidents/{id}`, `PATCH .../assign`, `PATCH .../status` | staff |
| Assets | `GET /assets`, `GET /assets/{id}` | all logged-in |
| | `POST /assets`, `PUT /assets/{id}` | admin, manager |
| Monitoring | `POST /monitoring/metrics`, `GET /monitoring/latest`, `GET /monitoring/assets/{id}/metrics`, `GET /monitoring/anomalies`, `GET /monitoring/stream` (SSE) | staff |
| Analytics | `GET /analytics/summary`, `/tickets/by-status`, `/by-priority`, `/by-category`, `/tickets/trend?granularity=day|month`, `/tickets/sla`, `/agents/workload`, `/incidents/severity`, `/assets/health`, `/ai/stats`, `/export/tickets.csv`, `/export/incidents.csv` | staff |
| AI | `GET /ai/status`, `POST /ai/classify`, `POST /ai/recommend` (503 if the AI service is down) | staff |
| Notifications | `GET /notifications`, `GET /unread-count`, `POST /{id}/read`, `POST /read-all` | own only |
| Audit | `GET /audit-logs` | admin, manager |

## Errors

JSON body `{timestamp, status, error, fieldErrors?}`. 400 validation or bad transition, 401 not authenticated, 403 not allowed, 404 not found, 409 data conflict, 503 AI service unavailable.

## Ticket status transitions

OPEN → IN_PROGRESS, ON_HOLD, RESOLVED · IN_PROGRESS → ON_HOLD, RESOLVED, OPEN · ON_HOLD → IN_PROGRESS, OPEN · RESOLVED → CLOSED, IN_PROGRESS · CLOSED is final.

## AI service (internal, port 8000)

`GET /health`, `POST /api/v1/ai/classify-incident`, `POST /api/v1/ai/detect-anomaly`, `POST /api/v1/ai/recommend-actions`, `GET /api/v1/ai/model-info`.

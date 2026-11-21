# Contributing

Thanks for taking an interest in EnterpriseOps AI. The project is a work in progress; keep changes and documentation aligned with the status in [PROJECT_STATUS.md](PROJECT_STATUS.md) and [docs/TESTING.md](docs/TESTING.md).

## Getting started

Follow the setup instructions in the [README](README.md). The full stack runs with Docker Compose; the services can also be started separately as described there.

## Checks

Run checks that cover your change and include their results in your pull request. The [testing report](docs/TESTING.md) records which checks have actually been run and which remain pending.

From the repository root, the documented checks include:

```bash
python ai-service/tests/test_models.py
python simulator/test_simulate.py
python database/validate_sqlite.py
```

For the service and UI, use the commands from the testing report:

```bash
cd ai-service && pytest
cd backend && mvn test
cd frontend && npm run lint && npm run build
```

The Docker-backed integration suite uses `mvn verify` from `backend`; it requires Docker. Playwright UI checks require frontend dependencies and the demo password configured as documented.

## Pull request checklist

- Keep changes focused and describe the behavior they affect.
- Add or update tests for behavior changes.
- Update the README or docs when setup, API behavior, project status or limitations change.
- Report checks that were skipped and why; do not describe an unrun check as passing.
- Use synthetic data only. Never commit `.env` files, credentials, tokens or private user data.

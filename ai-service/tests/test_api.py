"""API tests using FastAPI's TestClient. NOT EXECUTED in the build sandbox (fastapi/httpx/pytest unavailable)."""
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    assert client.get("/health").json() == {"status": "ok"}

def test_classify_returns_category_and_version():
    r = client.post("/api/v1/ai/classify-incident", json={"text": "vpn keeps dropping"})
    assert r.status_code == 200
    body = r.json()
    assert body["category"] == "network" and 0 < body["confidence"] <= 1 and body["model_version"]

def test_classify_rejects_too_short_text():
    assert client.post("/api/v1/ai/classify-incident", json={"text": "a"}).status_code == 422

def test_detect_anomaly_extreme_and_normal():
    bad = client.post("/api/v1/ai/detect-anomaly", json={"cpu": 99, "ram": 98, "disk": 60, "network_mbps": 550}).json()
    ok = client.post("/api/v1/ai/detect-anomaly", json={"cpu": 40, "ram": 55, "disk": 60, "network_mbps": 120}).json()
    assert bad["is_anomaly"] is True and ok["is_anomaly"] is False

def test_detect_anomaly_validates_ranges():
    assert client.post("/api/v1/ai/detect-anomaly", json={"cpu": 150, "ram": 1, "disk": 1, "network_mbps": 1}).status_code == 422

def test_recommend_actions_flags_human_review():
    body = client.post("/api/v1/ai/recommend-actions", json={"text": "forgot password"}).json()
    assert body["recommended_actions"] and "human review" in body["note"].lower()

def test_model_info_mentions_synthetic():
    assert "SYNTHETIC" in client.get("/api/v1/ai/model-info").json()["data"]

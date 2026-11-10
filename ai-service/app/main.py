import json
from pathlib import Path
import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field

ROOT = Path(__file__).resolve().parents[1]
clf = joblib.load(ROOT / "models/incident_classifier.joblib")
iso = joblib.load(ROOT / "models/anomaly_detector.joblib")
INFO = json.load(open(ROOT / "models/evaluation.json"))
app = FastAPI(title="EnterpriseOps AI Service")

ACTIONS = {  # static, human-reviewed suggestions (not model output)
 "network": ["Check VPN/Wi-Fi status and DNS", "Restart router or network adapter", "Escalate to network team if multiple users affected"],
 "hardware": ["Check power and cables", "Run hardware diagnostics", "Raise replacement request"],
 "software": ["Restart the application", "Check for pending updates", "Reinstall or repair the application"],
 "access": ["Verify user identity", "Reset password or MFA", "Check group permissions"],
 "database": ["Check disk space and running queries", "Review backup job logs", "Escalate to DBA if replication is lagging"],
}

class TextIn(BaseModel):
    text: str = Field(min_length=3, max_length=2000)

class MetricsIn(BaseModel):
    cpu: float = Field(ge=0, le=100)
    ram: float = Field(ge=0, le=100)
    disk: float = Field(ge=0, le=100)
    network_mbps: float = Field(ge=0)

@app.get("/health")
def health(): return {"status": "ok"}

@app.post("/api/v1/ai/classify-incident")
def classify(body: TextIn):
    probs = clf.predict_proba([body.text])[0]
    i = int(probs.argmax())
    return {"category": str(clf.classes_[i]), "confidence": round(float(probs[i]), 4),
            "model_version": INFO["model_version"]}

@app.post("/api/v1/ai/detect-anomaly")
def detect(body: MetricsIn):
    x = pd.DataFrame([{"cpu": body.cpu, "ram": body.ram, "disk": body.disk, "network_mbps": body.network_mbps}])
    return {"is_anomaly": bool(iso.predict(x)[0] == -1), "score": round(float(iso.decision_function(x)[0]), 4),
            "model_version": INFO["model_version"]}

@app.post("/api/v1/ai/recommend-actions")
def recommend(body: TextIn):
    c = classify(body)
    return {**c, "recommended_actions": ACTIONS[c["category"]], "note": "Suggestions are static and need human review."}

@app.get("/api/v1/ai/model-info")
def model_info(): return INFO

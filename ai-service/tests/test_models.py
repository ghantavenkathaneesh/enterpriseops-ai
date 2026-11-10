"""Model tests. Plain asserts, so they run with pytest or with `python tests/test_models.py`."""
import json
from pathlib import Path
import joblib

ROOT = Path(__file__).resolve().parents[1]
clf = joblib.load(ROOT / "models/incident_classifier.joblib")
iso = joblib.load(ROOT / "models/anomaly_detector.joblib")

def test_classifier_known_examples():
    cases = {"vpn keeps dropping": "network", "laptop will not turn on": "hardware",
             "outlook crashes on startup": "software", "forgot password": "access",
             "database backup failed overnight": "database"}
    for text, expected in cases.items():
        assert clf.predict([text])[0] == expected, text

def test_classifier_probabilities_are_valid():
    probs = clf.predict_proba(["printer is broken"])[0]
    assert abs(probs.sum() - 1) < 1e-6 and probs.max() > 0.2

def test_anomaly_detector_flags_extreme_reading():
    assert iso.predict([[99, 98, 60, 550]])[0] == -1

def test_anomaly_detector_accepts_normal_reading():
    assert iso.predict([[40, 55, 60, 120]])[0] == 1

def test_evaluation_report_is_labelled_synthetic():
    report = json.load(open(ROOT / "models/evaluation.json"))
    assert "SYNTHETIC" in report["data"]
    assert 0 <= report["classifier"]["accuracy"] <= 1

if __name__ == "__main__":
    for name, fn in list(globals().items()):
        if name.startswith("test_"):
            fn(); print("PASS", name)

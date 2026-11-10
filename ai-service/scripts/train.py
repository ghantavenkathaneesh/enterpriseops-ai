import json, joblib, pandas as pd
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.ensemble import IsolationForest
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score, precision_score, recall_score, f1_score
ROOT = Path(__file__).resolve().parents[1]; VERSION = "0.1.0-synthetic"
report = {"model_version": VERSION, "data": "SYNTHETIC - not real-world performance"}

t = pd.read_csv(ROOT/"data/tickets_synthetic.csv")
Xtr,Xte,ytr,yte = train_test_split(t.text, t.category, test_size=0.2, random_state=42, stratify=t.category)
clf = Pipeline([("tfidf", TfidfVectorizer(ngram_range=(1,2))), ("lr", LogisticRegression(max_iter=1000))]).fit(Xtr,ytr)
pred = clf.predict(Xte)
report["classifier"] = {"accuracy": round(accuracy_score(yte,pred),4),
    "macro_f1": round(f1_score(yte,pred,average="macro"),4),
    "per_class": classification_report(yte,pred,output_dict=True)}
joblib.dump(clf, ROOT/"models/incident_classifier.joblib")

m = pd.read_csv(ROOT/"data/metrics_synthetic.csv"); feats = ["cpu","ram","disk","network_mbps"]
tr,te = train_test_split(m, test_size=0.3, random_state=42, stratify=m.is_anomaly)
iso = IsolationForest(n_estimators=200, contamination=0.03, random_state=42).fit(tr[feats][tr.is_anomaly==0])
p = (iso.predict(te[feats])==-1).astype(int)
report["anomaly_detector"] = {"precision": round(precision_score(te.is_anomaly,p),4),
    "recall": round(recall_score(te.is_anomaly,p),4), "f1": round(f1_score(te.is_anomaly,p),4),
    "test_rows": len(te), "true_anomalies": int(te.is_anomaly.sum())}
joblib.dump(iso, ROOT/"models/anomaly_detector.joblib")
json.dump(report, open(ROOT/"models/evaluation.json","w"), indent=2)
c = report["classifier"]
print("classifier accuracy", c["accuracy"], "macro_f1", c["macro_f1"])
print("anomaly", report["anomaly_detector"])

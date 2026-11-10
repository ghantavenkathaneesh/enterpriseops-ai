"""Synthetic infrastructure metric simulator. Every reading is flagged simulated=true.

Logs in to the backend, then sends CPU/RAM/disk/network readings for every asset that is not a laptop.
About 4% of readings are injected anomalies so the AI/threshold pipeline has something to find.
Standard library only.  Usage:  python simulate.py --api http://localhost:8080 --email admin@enterpriseops.local
Password is read from the SIM_PASSWORD environment variable (never pass it on the command line)."""
import argparse, json, os, random, sys, time
import urllib.error, urllib.request

def normal_reading(rng):
    clip = lambda v, lo=0.0, hi=100.0: round(max(lo, min(hi, v)), 2)
    return {"cpu": clip(rng.gauss(40, 8)), "ram": clip(rng.gauss(55, 7)), "disk": clip(rng.gauss(60, 5)),
            "networkMbps": clip(rng.gauss(120, 25), 0, 10_000)}

def anomalous_reading(rng):
    r = normal_reading(rng)
    kind = rng.choice(["cpu", "ram", "network", "all"])
    if kind in ("cpu", "all"): r["cpu"] = round(rng.uniform(92, 100), 2)
    if kind in ("ram", "all"): r["ram"] = round(rng.uniform(90, 100), 2)
    if kind in ("network", "all"): r["networkMbps"] = round(rng.uniform(450, 700), 2)
    return r

def make_reading(rng, anomaly_rate):
    return anomalous_reading(rng) if rng.random() < anomaly_rate else normal_reading(rng)

def call(api, path, token=None, body=None):
    req = urllib.request.Request(api + path, method="POST" if body is not None else "GET",
                                 data=json.dumps(body).encode() if body is not None else None)
    req.add_header("Content-Type", "application/json")
    if token: req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.loads(r.read().decode() or "null")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--api", default="http://localhost:8080")
    ap.add_argument("--email", default="admin@enterpriseops.local")
    ap.add_argument("--interval", type=float, default=5.0)
    ap.add_argument("--anomaly-rate", type=float, default=0.04)
    ap.add_argument("--count", type=int, default=0, help="stop after N rounds (0 = run forever)")
    a = ap.parse_args()
    password = os.environ.get("SIM_PASSWORD")
    if not password: sys.exit("Set SIM_PASSWORD in the environment")
    rng = random.Random()
    token = call(a.api, "/api/v1/auth/login", body={"email": a.email, "password": password})["token"]
    assets = [x for x in call(a.api, "/api/v1/assets?size=100", token)["content"] if x["assetType"] != "LAPTOP" and x["status"] == "ACTIVE"]
    if not assets: sys.exit("No eligible assets found")
    print(f"Simulating {len(assets)} assets (SYNTHETIC data) every {a.interval}s. Ctrl+C to stop.")
    n = 0
    while a.count == 0 or n < a.count:
        for asset in assets:
            body = {"assetId": asset["id"], "simulated": True, **make_reading(rng, a.anomaly_rate)}
            try:
                res = call(a.api, "/api/v1/monitoring/metrics", token, body)
                flag = f"ANOMALY via {res['detectionSource']} incident #{res['incidentId']}" if res["anomaly"] else "ok"
                print(f"{asset['name']}: cpu={body['cpu']} ram={body['ram']} net={body['networkMbps']} -> {flag}")
            except urllib.error.URLError as e:
                print("backend unreachable:", e); time.sleep(5)
        n += 1; time.sleep(a.interval)

if __name__ == "__main__":
    main()

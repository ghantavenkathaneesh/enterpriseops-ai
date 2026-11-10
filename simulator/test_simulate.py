import random
from simulate import normal_reading, anomalous_reading, make_reading

def test_normal_readings_stay_in_valid_ranges():
    rng = random.Random(1)
    for _ in range(1000):
        r = normal_reading(rng)
        assert 0 <= r["cpu"] <= 100 and 0 <= r["ram"] <= 100 and 0 <= r["disk"] <= 100 and r["networkMbps"] >= 0

def test_anomalous_readings_breach_a_threshold():
    rng = random.Random(2)
    for _ in range(200):
        r = anomalous_reading(rng)
        assert r["cpu"] >= 92 or r["ram"] >= 90 or r["networkMbps"] >= 450

def test_anomaly_rate_is_roughly_respected():
    rng = random.Random(3)
    hits = sum(1 for _ in range(5000) if (lambda r: r["cpu"] >= 92 or r["ram"] >= 90 or r["networkMbps"] >= 450)(make_reading(rng, 0.04)))
    assert 0.02 < hits / 5000 < 0.09

if __name__ == "__main__":
    for n, f in list(globals().items()):
        if n.startswith("test_"): f(); print("PASS", n)

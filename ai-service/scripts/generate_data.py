"""Generate SYNTHETIC data. Nothing here is real company data."""
import random, numpy as np, pandas as pd
from pathlib import Path
random.seed(42); np.random.seed(42)
DATA = Path(__file__).resolve().parents[1] / "data"

CATS = {
 "network": (["vpn","wifi","network","internet","dns","router","connection"],
   ["cannot connect to {}","{} keeps dropping","{} is very slow","no access, {} timeout","{} unreachable from office"]),
 "hardware": (["laptop","printer","monitor","keyboard","mouse","docking station","battery"],
   ["{} not working","{} is broken","{} will not turn on","{} making strange noise","need replacement {}"]),
 "software": (["outlook","excel","browser","teams","antivirus","application","update"],
   ["{} crashes on startup","{} shows error message","{} freezes often","cannot install {}","{} not opening"]),
 "access": (["password","account","login","permissions","mfa","email access","shared folder"],
   ["forgot {}","{} locked out","need {} reset","{} denied","cannot sign in, {} issue"]),
 "database": (["database","sql query","report","backup","server disk","data export","replication"],
   ["{} is very slow","{} failed overnight","{} timeout error","{} not updating","error running {}"]),
}
FILLER = ["urgent","please help","since morning","happening again","for whole team","","","","after update","on my machine"]

rows = []
for cat,(nouns,tpls) in CATS.items():
    for _ in range(300):
        t = random.choice(tpls).format(random.choice(nouns))
        f = random.choice(FILLER)
        rows.append({"text": (t+" "+f).strip(), "category": cat})
df = pd.DataFrame(rows)
idx = df.sample(frac=0.04, random_state=1).index   # 4% label noise to mimic ambiguous tickets
df.loc[idx,"category"] = np.random.choice(list(CATS), len(idx))
df.to_csv(DATA/"tickets_synthetic.csv", index=False)

n = 3000
m = pd.DataFrame({"cpu": np.random.normal(40,8,n).clip(0,100),
                  "ram": np.random.normal(55,7,n).clip(0,100),
                  "disk": np.random.normal(60,5,n).clip(0,100),
                  "network_mbps": np.random.normal(120,25,n).clip(0,None)})
m["is_anomaly"] = 0
a = m.sample(n=90, random_state=3).index
m.loc[a,"cpu"] = np.random.uniform(88,100,len(a))
m.loc[a,"ram"] = np.random.uniform(85,100,len(a))
m.loc[a,"network_mbps"] = np.random.uniform(350,600,len(a))
m.loc[a,"is_anomaly"] = 1
m.to_csv(DATA/"metrics_synthetic.csv", index=False)
print("wrote", len(df), "tickets and", len(m), "metric rows (synthetic)")

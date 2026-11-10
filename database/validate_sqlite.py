"""Structural check of the PostgreSQL migrations by translating them to SQLite.
This does NOT prove PostgreSQL compatibility; it checks tables, constraints and portable reports."""
import re, sqlite3, sys
from pathlib import Path
d = Path(__file__).parent
def tr(sql):
    sql = re.sub(r"BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT", sql)
    sql = sql.replace("TIMESTAMPTZ","TEXT").replace("DEFAULT now()","DEFAULT CURRENT_TIMESTAMP")
    sql = re.sub(r"NUMERIC\(\d+,\d+\)","REAL",sql)
    sql = re.sub(r"DEFAULT TRUE","DEFAULT 1",sql); sql = re.sub(r"DEFAULT FALSE","DEFAULT 0",sql)
    return sql.replace(" DESC)",")")
db = sqlite3.connect(":memory:"); db.execute("PRAGMA foreign_keys=ON")
for f in sorted((d/"migrations").glob("V*.sql")): db.executescript(tr(f.read_text()))
tables = {r[0] for r in db.execute("select name from sqlite_master where type='table' and name not like 'sqlite_%'")}
need = {"roles","users","tickets","ticket_comments","incidents","assets","system_metrics","anomaly_events","ai_predictions","notifications","audit_logs","sla_policies"}
assert need <= tables, need - tables
print("tables ok:", len(tables))
db.execute("insert into users(email,full_name,password_hash,role_id) values('a@x.io','A','h',1)")
db.execute("insert into assets(asset_tag,name,asset_type) values('SRV-1','srv','SERVER')")
db.execute("insert into tickets(title,description,reporter_id,priority) values('t','d',1,'HIGH')")
db.execute("insert into incidents(title,description,severity,source,asset_id) values('i','d','HIGH','MONITORING',1)")
try:
    db.execute("insert into incidents(title,description,severity,source,asset_id) values('i2','d','HIGH','MONITORING',1)"); print("FAIL dedupe index")
    sys.exit(1)
except sqlite3.IntegrityError: print("dedupe unique index ok")
for bad,label in [("insert into tickets(title,description,reporter_id,priority) values('t','d',1,'URGENT')","priority check"),
                  ("insert into system_metrics(asset_id,cpu_percent,ram_percent,disk_percent,network_mbps) values(1,150,1,1,1)","metric range check"),
                  ("insert into tickets(title,description,reporter_id) values('t','d',999)","foreign key")]:
    try: db.execute(bad); print("FAIL",label); sys.exit(1)
    except sqlite3.IntegrityError: print(label,"ok")
print(db.execute("select status,count(*) from tickets group by status").fetchall())

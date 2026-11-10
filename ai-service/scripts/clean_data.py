"""Clean ticket text: lowercase, collapse whitespace, drop empty rows and exact duplicates.
Writes data/tickets_clean.csv. train.py currently reads the raw synthetic file; point it here to use cleaned data."""
import pandas as pd
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
df = pd.read_csv(ROOT / "data/tickets_synthetic.csv")
before = len(df)
df["text"] = df["text"].astype(str).str.lower().str.replace(r"\s+", " ", regex=True).str.strip()
df = df[df["text"].str.len() >= 3].drop_duplicates()
df.to_csv(ROOT / "data/tickets_clean.csv", index=False)
print(f"cleaned: {before} -> {len(df)} rows")

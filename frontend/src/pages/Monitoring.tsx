import { useEffect, useRef, useState } from 'react';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, TOKEN_KEY, useFetch } from '../api';
import { ErrorBox, fmt, Loading, PageHeader, SimulatedTag } from '../components/ui';
import type { AnomalyEvent, LatestMetric, MetricPoint, MetricResult } from '../types';

export default function Monitoring() {
  const latest = useFetch(() => api.get<LatestMetric[]>('/monitoring/latest').then((r) => r.data), []);
  const anomalies = useFetch(() => api.get<AnomalyEvent[]>('/monitoring/anomalies?limit=20').then((r) => r.data), []);
  const [selected, setSelected] = useState<number | null>(null);
  const [history, setHistory] = useState<MetricPoint[]>([]);
  const [live, setLive] = useState<'connecting' | 'live' | 'offline'>('connecting');
  const [banner, setBanner] = useState<string | null>(null);
  const [byAsset, setByAsset] = useState<Record<number, LatestMetric>>({});
  const selRef = useRef<number | null>(null);
  useEffect(() => { selRef.current = selected; }, [selected]);

  useEffect(() => {
    if (latest.data) {
      setByAsset(Object.fromEntries(latest.data.map((l) => [l.assetId, l])));
      if (selected === null && latest.data.length) setSelected(latest.data[0].assetId);
    }
  }, [latest.data, selected]);

  useEffect(() => {
    if (selected === null) return;
    api.get<MetricPoint[]>(`/monitoring/assets/${selected}/metrics?limit=60`).then((r) => setHistory(r.data.slice().reverse())).catch(() => setHistory([]));
  }, [selected]);

  // Server-sent events: the browser's EventSource cannot send an Authorization header, so the token goes in the query string.
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return;
    const es = new EventSource(`/api/v1/monitoring/stream?token=${encodeURIComponent(token)}`);
    es.onopen = () => setLive('live');
    es.onerror = () => setLive('offline');
    es.addEventListener('metric', (ev) => {
      const m = JSON.parse((ev as MessageEvent).data) as MetricResult;
      const point: MetricPoint = { id: Date.now(), cpu: m.cpu, ram: m.ram, disk: m.disk, networkMbps: m.networkMbps, simulated: m.simulated, recordedAt: m.recordedAt };
      setByAsset((prev) => ({ ...prev, [m.assetId]: { assetId: m.assetId, assetName: prev[m.assetId]?.assetName ?? `Asset ${m.assetId}`, assetType: prev[m.assetId]?.assetType ?? '', latest: point } }));
      if (selRef.current === m.assetId) setHistory((h) => [...h.slice(-59), point]);
    });
    es.addEventListener('incident', (ev) => {
      const d = JSON.parse((ev as MessageEvent).data) as { incidentId: number; assetName: string };
      setBanner(`New incident #${d.incidentId} on ${d.assetName}`);
      anomalies.reload();
    });
    return () => es.close();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (latest.loading) return <Loading />;
  if (latest.error) return <ErrorBox message={latest.error} onRetry={latest.reload} />;
  const cards = Object.values(byAsset);
  return (
    <>
      <PageHeader title="Monitoring"><span className={`badge b-${live === 'live' ? 'active' : 'critical'}`}>{live === 'live' ? 'Live' : live === 'connecting' ? 'Connecting…' : 'Disconnected'}</span></PageHeader>
      {banner && <div className="state error" role="alert">{banner} <button className="btn small" onClick={() => setBanner(null)}>Dismiss</button></div>}
      {cards.length === 0 && <div className="state">No readings yet. Start the simulator (see README) to generate synthetic metrics.</div>}
      <div className="grid stats">
        {cards.map((c) => (
          <button key={c.assetId} className={`card asset-card ${selected === c.assetId ? 'selected' : ''}`} onClick={() => setSelected(c.assetId)}>
            <strong>{c.assetName}</strong>{c.latest.simulated && <SimulatedTag />}
            <div>CPU {c.latest.cpu}% · RAM {c.latest.ram}%</div><div>Disk {c.latest.disk}% · Net {c.latest.networkMbps} Mbps</div>
            <small className="muted">{fmt(c.latest.recordedAt)}</small>
          </button>
        ))}
      </div>
      <div className="card"><h2>History {selected !== null && byAsset[selected] ? `· ${byAsset[selected].assetName}` : ''}</h2>
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={history.map((p) => ({ t: new Date(p.recordedAt).toLocaleTimeString(), cpu: p.cpu, ram: p.ram, disk: p.disk }))}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="t" /><YAxis domain={[0, 100]} /><Tooltip /><Legend />
            <Line dataKey="cpu" stroke="#2f6fed" dot={false} isAnimationActive={false} /><Line dataKey="ram" stroke="#16a34a" dot={false} isAnimationActive={false} /><Line dataKey="disk" stroke="#d97706" dot={false} isAnimationActive={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <div className="card table-wrap"><h2>Recent anomalies</h2>
        <table><thead><tr><th>When</th><th>Asset</th><th>Detected by</th><th>Reason</th><th>Incident</th></tr></thead>
          <tbody>{anomalies.data?.map((a) => <tr key={a.id}><td>{fmt(a.createdAt)}</td><td>{byAsset[a.assetId]?.assetName ?? a.assetId}</td><td>{a.detectionSource}</td><td>{a.reason}</td><td>{a.incidentId ? `#${a.incidentId}` : '—'}</td></tr>)}
            {anomalies.data?.length === 0 && <tr><td colSpan={5} className="muted">No anomalies recorded.</td></tr>}</tbody></table>
      </div>
    </>
  );
}

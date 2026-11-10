import axios from 'axios';
import { useState, type FormEvent } from 'react';
import { api, errorMessage, useFetch } from '../api';
import { ErrorBox, Loading, PageHeader } from '../components/ui';

interface Stat { kind: string; label: string; total: number; avg_confidence: number | null }
interface Reco { category: string; confidence: number; model_version: string; recommended_actions: string[]; note: string }

export default function AiInsights() {
  const stats = useFetch(() => api.get<Stat[]>('/analytics/ai/stats').then((r) => r.data), []);
  const status = useFetch(() => api.get<{ aiServiceHealthy: boolean }>('/ai/status').then((r) => r.data), []);
  const [text, setText] = useState(''); const [result, setResult] = useState<Reco | null>(null);
  const [error, setError] = useState<string | null>(null); const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setError(null); setResult(null);
    try { setResult((await api.post<Reco>('/ai/recommend', { text })).data); }
    catch (err) { setError(axios.isAxiosError(err) && err.response?.status === 503 ? 'The AI service is currently unavailable. Tickets and monitoring keep working without it.' : errorMessage(err)); }
    finally { setBusy(false); }
  };
  return (
    <>
      <PageHeader title="AI Insights">
        {status.data && <span className={`badge b-${status.data.aiServiceHealthy ? 'active' : 'critical'}`}>AI service {status.data.aiServiceHealthy ? 'online' : 'offline'}</span>}
      </PageHeader>
      <div className="state">Models are trained on <strong>synthetic</strong> data. Accuracy figures are not real-world performance. Suggestions need human review.</div>
      <div className="card"><h2>Try the classifier</h2>
        <form className="form" onSubmit={submit}>
          <textarea value={text} onChange={(e) => setText(e.target.value)} rows={3} minLength={3} required placeholder="Describe an issue, e.g. “VPN keeps dropping since morning”" aria-label="Issue description" />
          <button className="btn primary" disabled={busy}>{busy ? 'Analysing…' : 'Classify and suggest actions'}</button>
        </form>
        {error && <div className="state error" role="alert">{error}</div>}
        {result && <div className="result"><p><strong>Category:</strong> {result.category} ({(result.confidence * 100).toFixed(1)}% confidence, model {result.model_version})</p>
          <ul>{result.recommended_actions.map((a) => <li key={a}>{a}</li>)}</ul><small className="muted">{result.note}</small></div>}
      </div>
      <div className="card table-wrap"><h2>Prediction statistics (from the database)</h2>
        {stats.loading && <Loading />}{stats.error && <ErrorBox message={stats.error} onRetry={stats.reload} />}
        {stats.data && <table><thead><tr><th>Kind</th><th>Label</th><th>Count</th><th>Avg confidence</th></tr></thead>
          <tbody>{stats.data.map((s) => <tr key={`${s.kind}-${s.label}`}><td>{s.kind}</td><td>{s.label}</td><td>{s.total}</td><td>{s.avg_confidence ?? '—'}</td></tr>)}
            {stats.data.length === 0 && <tr><td colSpan={4} className="muted">No predictions stored yet. Create a ticket without choosing a category.</td></tr>}</tbody></table>}
      </div>
    </>
  );
}

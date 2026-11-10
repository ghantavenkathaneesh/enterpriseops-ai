import { useState, type FormEvent } from 'react';
import { api, errorMessage, useFetch } from '../api';
import { Badge, ErrorBox, fmt, Loading, Modal, PageHeader, Pager } from '../components/ui';
import { INCIDENT_TRANSITIONS, type Incident, type IncidentStatus, type Page, type Priority, type User } from '../types';

const SEVERITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUSES: IncidentStatus[] = ['OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED'];

export default function Incidents() {
  const [status, setStatus] = useState(''); const [severity, setSeverity] = useState(''); const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Incident | null>(null); const [creating, setCreating] = useState(false);
  const list = useFetch(() => api.get<Page<Incident>>('/incidents', { params: { status: status || undefined, severity: severity || undefined, page, size: 15 } }).then((r) => r.data), [status, severity, page]);

  return (
    <>
      <PageHeader title="Incidents"><button className="btn primary" onClick={() => setCreating(true)}>New incident</button></PageHeader>
      <div className="filters">
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Status filter"><option value="">All statuses</option>{STATUSES.map((s) => <option key={s}>{s}</option>)}</select>
        <select value={severity} onChange={(e) => { setSeverity(e.target.value); setPage(0); }} aria-label="Severity filter"><option value="">All severities</option>{SEVERITIES.map((s) => <option key={s}>{s}</option>)}</select>
      </div>
      {list.loading && <Loading />}{list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
      {list.data && (
        <div className="card table-wrap">
          <table>
            <thead><tr><th>#</th><th>Title</th><th>Severity</th><th>Status</th><th>Source</th><th>Asset</th><th>Assignee</th><th>Created</th></tr></thead>
            <tbody>
              {list.data.content.map((i) => (
                <tr key={i.id} className="clickable" onClick={() => setSelected(i)} data-testid={`incident-row-${i.id}`}>
                  <td>{i.id}</td><td>{i.title}</td><td><Badge value={i.severity} /></td><td><Badge value={i.status} /></td><td>{i.source}</td>
                  <td>{i.assetName ?? '—'}</td><td>{i.assigneeName ?? '—'}</td><td>{fmt(i.createdAt)}</td>
                </tr>
              ))}
              {list.data.content.length === 0 && <tr><td colSpan={8} className="muted">No incidents.</td></tr>}
            </tbody>
          </table>
          <Pager page={list.data} onChange={setPage} />
        </div>
      )}
      {creating && <CreateIncident onClose={() => setCreating(false)} onCreated={() => { setCreating(false); list.reload(); }} />}
      {selected && <IncidentDetail incident={selected} onClose={() => setSelected(null)} onChanged={() => { setSelected(null); list.reload(); }} />}
    </>
  );
}

function CreateIncident({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [title, setTitle] = useState(''); const [description, setDescription] = useState(''); const [severity, setSeverity] = useState<Priority>('MEDIUM');
  const [error, setError] = useState<string | null>(null);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setError(null);
    try { await api.post('/incidents', { title, description, severity }); onCreated(); } catch (err) { setError(errorMessage(err)); }
  };
  return (
    <Modal title="New incident" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>Title<input value={title} onChange={(e) => setTitle(e.target.value)} required /></label>
        <label>Description<textarea value={description} onChange={(e) => setDescription(e.target.value)} required rows={4} /></label>
        <label>Severity<select value={severity} onChange={(e) => setSeverity(e.target.value as Priority)}>{SEVERITIES.map((s) => <option key={s}>{s}</option>)}</select></label>
        {error && <div className="state error" role="alert">{error}</div>}
        <button className="btn primary">Create incident</button>
      </form>
    </Modal>
  );
}

function IncidentDetail({ incident, onClose, onChanged }: { incident: Incident; onClose: () => void; onChanged: () => void }) {
  const staff = useFetch(() => api.get<User[]>('/users/staff').then((r) => r.data), []);
  const [notes, setNotes] = useState(''); const [error, setError] = useState<string | null>(null);
  const act = async (fn: () => Promise<unknown>) => { setError(null); try { await fn(); onChanged(); } catch (e) { setError(errorMessage(e)); } };
  return (
    <Modal title={`Incident #${incident.id}`} onClose={onClose}>
      <div className="detail">
        <h3>{incident.title}</h3><p>{incident.description}</p>
        <p><Badge value={incident.severity} /> <Badge value={incident.status} /> <span className="muted">Source: {incident.source} · Asset: {incident.assetName ?? '—'}</span></p>
        {incident.resolutionNotes && <p><strong>Resolution:</strong> {incident.resolutionNotes}</p>}
        <div className="filters">
          <select aria-label="Assign to" defaultValue="" onChange={(e) => e.target.value && act(() => api.patch(`/incidents/${incident.id}/assign`, { assigneeId: Number(e.target.value) }))}>
            <option value="">{incident.assigneeName ?? 'Assign to…'}</option>{staff.data?.map((a) => <option key={a.id} value={a.id}>{a.fullName}</option>)}
          </select>
          <select aria-label="Change status" defaultValue="" onChange={(e) => e.target.value && act(() => api.patch(`/incidents/${incident.id}/status`, { status: e.target.value, resolutionNotes: notes || null }))}>
            <option value="">Change status…</option>{INCIDENT_TRANSITIONS[incident.status].map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <label>Resolution notes (required when resolving)<textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} /></label>
        {error && <div className="state error" role="alert">{error}</div>}
      </div>
    </Modal>
  );
}

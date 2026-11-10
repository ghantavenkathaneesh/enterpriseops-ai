import { useState, type FormEvent } from 'react';
import { api, errorMessage, useFetch } from '../api';
import { useAuth } from '../auth';
import { Badge, ErrorBox, fmt, Loading, Modal, PageHeader, Pager } from '../components/ui';
import { isStaff, TICKET_TRANSITIONS, type Asset, type Page, type Priority, type Ticket, type TicketComment, type TicketStatus, type User } from '../types';

const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED'];
const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const CATEGORIES = ['network', 'hardware', 'software', 'access', 'database', 'other'];

export default function Tickets() {
  const { user } = useAuth();
  const [q, setQ] = useState(''); const [status, setStatus] = useState(''); const [priority, setPriority] = useState(''); const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<number | null>(null);

  const list = useFetch(() => api.get<Page<Ticket>>('/tickets', { params: { q: q || undefined, status: status || undefined, priority: priority || undefined, category: category || undefined, page, size: 15 } }).then((r) => r.data), [q, status, priority, category, page]);

  return (
    <>
      <PageHeader title={user?.role === 'EMPLOYEE' ? 'My tickets' : 'Tickets'}>
        <button className="btn primary" onClick={() => setCreating(true)}>New ticket</button>
      </PageHeader>
      <div className="filters">
        <input placeholder="Search title or description" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} aria-label="Search tickets" />
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Status filter"><option value="">All statuses</option>{STATUSES.map((s) => <option key={s}>{s}</option>)}</select>
        <select value={priority} onChange={(e) => { setPriority(e.target.value); setPage(0); }} aria-label="Priority filter"><option value="">All priorities</option>{PRIORITIES.map((s) => <option key={s}>{s}</option>)}</select>
        <select value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }} aria-label="Category filter"><option value="">All categories</option>{CATEGORIES.map((s) => <option key={s}>{s}</option>)}</select>
      </div>
      {list.loading && <Loading />}
      {list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
      {list.data && (
        <div className="card table-wrap">
          <table>
            <thead><tr><th>#</th><th>Title</th><th>Category</th><th>Priority</th><th>Status</th><th>Assignee</th><th>Created</th></tr></thead>
            <tbody>
              {list.data.content.map((t) => (
                <tr key={t.id} className="clickable" onClick={() => setSelected(t.id)} data-testid={`ticket-row-${t.id}`}>
                  <td>{t.id}</td><td>{t.title}{t.slaBreached && <span className="badge b-critical" title="SLA breached">SLA</span>}</td><td>{t.category}</td>
                  <td><Badge value={t.priority} /></td><td><Badge value={t.status} /></td><td>{t.assigneeName ?? '—'}</td><td>{fmt(t.createdAt)}</td>
                </tr>
              ))}
              {list.data.content.length === 0 && <tr><td colSpan={7} className="muted">No tickets match.</td></tr>}
            </tbody>
          </table>
          <Pager page={list.data} onChange={setPage} />
        </div>
      )}
      {creating && <CreateTicket onClose={() => setCreating(false)} onCreated={() => { setCreating(false); list.reload(); }} />}
      {selected !== null && <TicketDetail id={selected} onClose={() => setSelected(null)} onChanged={list.reload} />}
    </>
  );
}

function CreateTicket({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [title, setTitle] = useState(''); const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM'); const [category, setCategory] = useState(''); const [assetId, setAssetId] = useState('');
  const [error, setError] = useState<string | null>(null); const [busy, setBusy] = useState(false);
  const assets = useFetch(() => api.get<Page<Asset>>('/assets', { params: { size: 100 } }).then((r) => r.data.content), []);

  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true); setError(null);
    try {
      await api.post('/tickets', { title, description, priority, category: category || null, assetId: assetId ? Number(assetId) : null });
      onCreated();
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };
  return (
    <Modal title="New ticket" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>Title<input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} /></label>
        <label>Description<textarea value={description} onChange={(e) => setDescription(e.target.value)} required rows={4} /></label>
        <label>Priority<select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>{PRIORITIES.map((p) => <option key={p}>{p}</option>)}</select></label>
        <label>Category<select value={category} onChange={(e) => setCategory(e.target.value)}><option value="">Auto-detect (AI, falls back to “other”)</option>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select></label>
        <label>Related asset (optional)<select value={assetId} onChange={(e) => setAssetId(e.target.value)}><option value="">None</option>{assets.data?.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}</select></label>
        {error && <div className="state error" role="alert">{error}</div>}
        <button className="btn primary" disabled={busy}>{busy ? 'Creating…' : 'Create ticket'}</button>
      </form>
    </Modal>
  );
}

function TicketDetail({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const { user } = useAuth();
  const staff = isStaff(user?.role);
  const t = useFetch(() => api.get<Ticket>(`/tickets/${id}`).then((r) => r.data), [id]);
  const comments = useFetch(() => api.get<TicketComment[]>(`/tickets/${id}/comments`).then((r) => r.data), [id]);
  const agents = useFetch(() => (staff ? api.get<User[]>('/users/staff').then((r) => r.data) : Promise.resolve([] as User[])), [staff]);
  const [body, setBody] = useState(''); const [error, setError] = useState<string | null>(null);

  const act = async (fn: () => Promise<unknown>) => {
    setError(null);
    try { await fn(); t.reload(); comments.reload(); onChanged(); } catch (e) { setError(errorMessage(e)); }
  };
  return (
    <Modal title={`Ticket #${id}`} onClose={onClose}>
      {t.loading && <Loading />}
      {t.error && <ErrorBox message={t.error} />}
      {t.data && (
        <div className="detail">
          <h3>{t.data.title}</h3>
          <p>{t.data.description}</p>
          <p className="muted">Reporter: {t.data.reporterName} · Category: {t.data.category} · Due: {fmt(t.data.dueAt)} · Asset: {t.data.assetName ?? '—'}</p>
          <p><Badge value={t.data.priority} /> <Badge value={t.data.status} /> {t.data.slaBreached && <span className="badge b-critical">SLA breached</span>}</p>
          {staff && (
            <div className="filters">
              <select aria-label="Change status" value="" onChange={(e) => e.target.value && act(() => api.patch(`/tickets/${id}/status`, { status: e.target.value }))}>
                <option value="">Change status…</option>{TICKET_TRANSITIONS[t.data.status].map((s) => <option key={s}>{s}</option>)}
              </select>
              <select aria-label="Assign to" value={t.data.assigneeId ?? ''} onChange={(e) => e.target.value && act(() => api.patch(`/tickets/${id}/assign`, { assigneeId: Number(e.target.value) }))}>
                <option value="">{t.data.assigneeName ?? 'Assign to…'}</option>{agents.data?.map((a) => <option key={a.id} value={a.id}>{a.fullName}</option>)}
              </select>
            </div>
          )}
          {error && <div className="state error" role="alert">{error}</div>}
          <h3>Comments</h3>
          {comments.data?.map((c) => <div key={c.id} className="comment"><strong>{c.authorName}</strong> <small>{fmt(c.createdAt)}</small><div>{c.body}</div></div>)}
          {comments.data?.length === 0 && <p className="muted">No comments yet.</p>}
          <form className="form" onSubmit={(e) => { e.preventDefault(); if (body.trim()) act(async () => { await api.post(`/tickets/${id}/comments`, { body }); setBody(''); }); }}>
            <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={2} placeholder="Add a comment" aria-label="Comment" />
            <button className="btn">Add comment</button>
          </form>
        </div>
      )}
    </Modal>
  );
}

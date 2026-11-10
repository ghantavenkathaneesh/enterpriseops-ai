import { useState, type FormEvent } from 'react';
import { api, errorMessage, useFetch } from '../api';
import { useAuth } from '../auth';
import { Badge, ErrorBox, Loading, Modal, PageHeader, Pager } from '../components/ui';
import type { Asset, Page } from '../types';

const TYPES = ['SERVER', 'LAPTOP', 'NETWORK', 'DATABASE', 'OTHER'];
const STATUSES = ['ACTIVE', 'MAINTENANCE', 'RETIRED'];

export default function Assets() {
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const [q, setQ] = useState(''); const [type, setType] = useState(''); const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Asset | 'new' | null>(null);
  const list = useFetch(() => api.get<Page<Asset>>('/assets', { params: { q: q || undefined, type: type || undefined, page, size: 15 } }).then((r) => r.data), [q, type, page]);
  return (
    <>
      <PageHeader title="Assets">{canEdit && <button className="btn primary" onClick={() => setEditing('new')}>Register asset</button>}</PageHeader>
      <div className="filters">
        <input placeholder="Search name or tag" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} aria-label="Search assets" />
        <select value={type} onChange={(e) => { setType(e.target.value); setPage(0); }} aria-label="Type filter"><option value="">All types</option>{TYPES.map((t) => <option key={t}>{t}</option>)}</select>
      </div>
      {list.loading && <Loading />}{list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
      {list.data && (
        <div className="card table-wrap">
          <table>
            <thead><tr><th>Tag</th><th>Name</th><th>Type</th><th>Status</th><th>Owner</th><th>Location</th>{canEdit && <th />}</tr></thead>
            <tbody>
              {list.data.content.map((a) => (
                <tr key={a.id}><td>{a.assetTag}</td><td>{a.name}</td><td>{a.assetType}</td><td><Badge value={a.status} /></td><td>{a.ownerName ?? '—'}</td><td>{a.location ?? '—'}</td>
                  {canEdit && <td><button className="btn small" onClick={() => setEditing(a)}>Edit</button></td>}</tr>
              ))}
              {list.data.content.length === 0 && <tr><td colSpan={7} className="muted">No assets.</td></tr>}
            </tbody>
          </table>
          <Pager page={list.data} onChange={setPage} />
        </div>
      )}
      {editing && <AssetForm asset={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); list.reload(); }} />}
    </>
  );
}

function AssetForm({ asset, onClose, onSaved }: { asset: Asset | null; onClose: () => void; onSaved: () => void }) {
  const [assetTag, setTag] = useState(asset?.assetTag ?? ''); const [name, setName] = useState(asset?.name ?? '');
  const [assetType, setType] = useState(asset?.assetType ?? 'SERVER'); const [status, setStatus] = useState(asset?.status ?? 'ACTIVE');
  const [location, setLocation] = useState(asset?.location ?? ''); const [error, setError] = useState<string | null>(null);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setError(null);
    const body = { assetTag, name, assetType, status, location: location || null, ownerId: asset?.ownerId ?? null };
    try { await (asset ? api.put(`/assets/${asset.id}`, body) : api.post('/assets', body)); onSaved(); } catch (err) { setError(errorMessage(err)); }
  };
  return (
    <Modal title={asset ? 'Edit asset' : 'Register asset'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>Asset tag<input value={assetTag} onChange={(e) => setTag(e.target.value)} required /></label>
        <label>Name<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Type<select value={assetType} onChange={(e) => setType(e.target.value)}>{TYPES.map((t) => <option key={t}>{t}</option>)}</select></label>
        <label>Status<select value={status} onChange={(e) => setStatus(e.target.value)}>{STATUSES.map((t) => <option key={t}>{t}</option>)}</select></label>
        <label>Location<input value={location} onChange={(e) => setLocation(e.target.value)} /></label>
        {error && <div className="state error" role="alert">{error}</div>}
        <button className="btn primary">Save</button>
      </form>
    </Modal>
  );
}

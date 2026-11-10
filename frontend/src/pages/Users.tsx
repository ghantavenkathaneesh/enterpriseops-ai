import { useState, type FormEvent } from 'react';
import { api, errorMessage, useFetch } from '../api';
import { useAuth } from '../auth';
import { Badge, ErrorBox, Loading, Modal, PageHeader, Pager } from '../components/ui';
import type { Page, Role, User } from '../types';

const ROLES: Role[] = ['ADMIN', 'MANAGER', 'SUPPORT_AGENT', 'EMPLOYEE'];

export default function Users() {
  const { user: me } = useAuth();
  const admin = me?.role === 'ADMIN';
  const [q, setQ] = useState(''); const [role, setRole] = useState(''); const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false); const [err, setErr] = useState<string | null>(null);
  const list = useFetch(() => api.get<Page<User>>('/users', { params: { q: q || undefined, role: role || undefined, page, size: 15 } }).then((r) => r.data), [q, role, page]);
  const update = async (id: number, body: { role?: Role; active?: boolean }) => { setErr(null); try { await api.put(`/users/${id}`, body); list.reload(); } catch (e) { setErr(errorMessage(e)); } };
  return (
    <>
      <PageHeader title="Users">{admin && <button className="btn primary" onClick={() => setCreating(true)}>New user</button>}</PageHeader>
      <div className="filters">
        <input placeholder="Search name or email" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} aria-label="Search users" />
        <select value={role} onChange={(e) => { setRole(e.target.value); setPage(0); }} aria-label="Role filter"><option value="">All roles</option>{ROLES.map((r) => <option key={r}>{r}</option>)}</select>
      </div>
      {err && <div className="state error" role="alert">{err}</div>}
      {list.loading && <Loading />}{list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
      {list.data && (
        <div className="card table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th>{admin && <th />}</tr></thead>
            <tbody>
              {list.data.content.map((u) => (
                <tr key={u.id}><td>{u.fullName}</td><td>{u.email}</td>
                  <td>{admin && u.id !== me?.id ? <select value={u.role} aria-label={`Role for ${u.email}`} onChange={(e) => update(u.id, { role: e.target.value as Role })}>{ROLES.map((r) => <option key={r}>{r}</option>)}</select> : <Badge value={u.role} />}</td>
                  <td><Badge value={u.active ? 'ACTIVE' : 'INACTIVE'} /></td>
                  {admin && <td>{u.id !== me?.id && <button className="btn small" onClick={() => update(u.id, { active: !u.active })}>{u.active ? 'Deactivate' : 'Activate'}</button>}</td>}</tr>
              ))}
            </tbody>
          </table>
          <Pager page={list.data} onChange={setPage} />
        </div>
      )}
      {creating && <CreateUser onClose={() => setCreating(false)} onCreated={() => { setCreating(false); list.reload(); }} />}
    </>
  );
}

function CreateUser({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [email, setEmail] = useState(''); const [fullName, setName] = useState(''); const [password, setPassword] = useState(''); const [role, setRole] = useState<Role>('EMPLOYEE');
  const [error, setError] = useState<string | null>(null);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setError(null);
    try { await api.post('/users', { email, fullName, password, role }); onCreated(); } catch (err) { setError(errorMessage(err)); }
  };
  return (
    <Modal title="New user" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>Full name<input value={fullName} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
        <label>Temporary password (min 8)<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} autoComplete="new-password" /></label>
        <label>Role<select value={role} onChange={(e) => setRole(e.target.value as Role)}>{ROLES.map((r) => <option key={r}>{r}</option>)}</select></label>
        {error && <div className="state error" role="alert">{error}</div>}
        <button className="btn primary">Create user</button>
      </form>
    </Modal>
  );
}

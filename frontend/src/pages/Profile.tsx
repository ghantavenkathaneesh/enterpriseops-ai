import { useState, type FormEvent } from 'react';
import { api, errorMessage } from '../api';
import { useAuth } from '../auth';
import { Badge, PageHeader } from '../components/ui';
import type { User } from '../types';

export default function Profile() {
  const { user, setUser } = useAuth();
  const [name, setName] = useState(user?.fullName ?? ''); const [msg, setMsg] = useState<string | null>(null); const [error, setError] = useState<string | null>(null);
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setMsg(null); setError(null);
    try { setUser((await api.put<User>('/auth/me', { fullName: name })).data); setMsg('Profile updated'); } catch (err) { setError(errorMessage(err)); }
  };
  return (
    <>
      <PageHeader title="Profile" />
      <form className="card form" onSubmit={submit}>
        <p><strong>{user?.email}</strong> <Badge value={user?.role ?? ''} /></p>
        <label>Full name<input value={name} onChange={(e) => setName(e.target.value)} required maxLength={150} /></label>
        {msg && <div className="state ok">{msg}</div>}{error && <div className="state error" role="alert">{error}</div>}
        <button className="btn primary">Save</button>
      </form>
    </>
  );
}

import { useState } from 'react';
import { api, errorMessage, useFetch } from '../api';
import { ErrorBox, fmt, Loading, PageHeader, Pager } from '../components/ui';
import type { AppNotification, Page } from '../types';

export default function Notifications() {
  const [page, setPage] = useState(0); const [err, setErr] = useState<string | null>(null);
  const list = useFetch(() => api.get<Page<AppNotification>>('/notifications', { params: { page, size: 20 } }).then((r) => r.data), [page]);
  const run = async (fn: () => Promise<unknown>) => { setErr(null); try { await fn(); list.reload(); } catch (e) { setErr(errorMessage(e)); } };
  return (
    <>
      <PageHeader title="Notifications"><button className="btn" onClick={() => run(() => api.post('/notifications/read-all'))}>Mark all as read</button></PageHeader>
      {err && <div className="state error" role="alert">{err}</div>}
      {list.loading && <Loading />}{list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
      {list.data && (
        <div className="card">
          {list.data.content.map((n) => (
            <div key={n.id} className={`notif ${n.read ? '' : 'unread'}`}>
              <div><strong>{n.type.replace('_', ' ')}</strong><div>{n.message}</div><small className="muted">{fmt(n.createdAt)}</small></div>
              {!n.read && <button className="btn small" onClick={() => run(() => api.post(`/notifications/${n.id}/read`))}>Mark read</button>}
            </div>
          ))}
          {list.data.content.length === 0 && <p className="muted">You have no notifications.</p>}
          <Pager page={list.data} onChange={setPage} />
        </div>
      )}
    </>
  );
}

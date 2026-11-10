import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, useFetch } from '../api';
import { ErrorBox, Loading, PageHeader, StatCard } from '../components/ui';
import type { LabelCount } from '../types';

interface Summary { totalTickets: number; openTickets: number; resolvedTickets: number; activeIncidents: number; avgResolutionHours: number | null; slaCompliancePercent: number | null; anomaliesLast24h: number }

export default function Dashboard() {
  const s = useFetch(() => api.get<Summary>('/analytics/summary').then((r) => r.data), []);
  const status = useFetch(() => api.get<LabelCount[]>('/analytics/tickets/by-status').then((r) => r.data), []);
  const trend = useFetch(() => api.get<LabelCount[]>('/analytics/tickets/trend?granularity=day').then((r) => r.data), []);
  const sev = useFetch(() => api.get<LabelCount[]>('/analytics/incidents/severity').then((r) => r.data), []);

  if (s.loading) return <Loading />;
  if (s.error || !s.data) return <ErrorBox message={s.error ?? 'No data'} onRetry={s.reload} />;
  const d = s.data;
  return (
    <>
      <PageHeader title="Overview" />
      <div className="grid stats">
        <StatCard label="Total tickets" value={d.totalTickets} />
        <StatCard label="Open tickets" value={d.openTickets} />
        <StatCard label="Active incidents" value={d.activeIncidents} />
        <StatCard label="Avg resolution" value={d.avgResolutionHours == null ? '—' : `${d.avgResolutionHours} h`} />
        <StatCard label="SLA compliance" value={d.slaCompliancePercent == null ? '—' : `${d.slaCompliancePercent}%`} hint="Resolved tickets within SLA" />
        <StatCard label="Anomalies (24h)" value={d.anomaliesLast24h} />
      </div>
      <div className="grid two">
        <div className="card"><h2>Tickets by status</h2>
          {status.data ? <ResponsiveContainer width="100%" height={240}><BarChart data={status.data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="total" fill="#2f6fed" /></BarChart></ResponsiveContainer> : <Loading />}
        </div>
        <div className="card"><h2>Incidents by severity</h2>
          {sev.data ? <ResponsiveContainer width="100%" height={240}><BarChart data={sev.data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="total" fill="#e5484d" /></BarChart></ResponsiveContainer> : <Loading />}
        </div>
      </div>
      <div className="card"><h2>Tickets created per day</h2>
        {trend.data ? <ResponsiveContainer width="100%" height={260}><LineChart data={trend.data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Line type="monotone" dataKey="total" stroke="#2f6fed" dot={false} /></LineChart></ResponsiveContainer> : <Loading />}
      </div>
    </>
  );
}

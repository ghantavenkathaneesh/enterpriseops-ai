import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, downloadCsv, errorMessage, useFetch } from '../api';
import { ErrorBox, Loading, PageHeader, StatCard } from '../components/ui';
import type { LabelCount } from '../types';

function Chart({ title, path, color }: { title: string; path: string; color: string }) {
  const d = useFetch(() => api.get<LabelCount[]>(path).then((r) => r.data), [path]);
  return (
    <div className="card"><h2>{title}</h2>
      {d.loading && <Loading />}{d.error && <ErrorBox message={d.error} onRetry={d.reload} />}
      {d.data && <ResponsiveContainer width="100%" height={230}><BarChart data={d.data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="total" fill={color} /></BarChart></ResponsiveContainer>}
    </div>
  );
}

export default function Reports() {
  const [gran, setGran] = useState<'day' | 'month'>('day');
  const [err, setErr] = useState<string | null>(null);
  const trend = useFetch(() => api.get<LabelCount[]>(`/analytics/tickets/trend?granularity=${gran}`).then((r) => r.data), [gran]);
  const sla = useFetch(() => api.get<{ withinSla: number; resolvedTotal: number; compliancePercent: number | null }>('/analytics/tickets/sla').then((r) => r.data), []);
  const dl = async (path: string, name: string) => { setErr(null); try { await downloadCsv(path, name); } catch (e) { setErr(errorMessage(e)); } };
  return (
    <>
      <PageHeader title="Analytics and reports">
        <button className="btn" onClick={() => dl('/analytics/export/tickets.csv', 'tickets.csv')}>Export tickets CSV</button>
        <button className="btn" onClick={() => dl('/analytics/export/incidents.csv', 'incidents.csv')}>Export incidents CSV</button>
      </PageHeader>
      {err && <div className="state error" role="alert">{err}</div>}
      {sla.data && <div className="grid stats"><StatCard label="SLA compliance" value={sla.data.compliancePercent == null ? '—' : `${sla.data.compliancePercent}%`} hint={`${sla.data.withinSla} of ${sla.data.resolvedTotal} resolved tickets within SLA`} /></div>}
      <div className="grid two">
        <Chart title="Tickets by status" path="/analytics/tickets/by-status" color="#2f6fed" />
        <Chart title="Tickets by priority" path="/analytics/tickets/by-priority" color="#d97706" />
        <Chart title="Tickets by category" path="/analytics/tickets/by-category" color="#16a34a" />
        <Chart title="Agent workload (active tickets)" path="/analytics/agents/workload" color="#7c3aed" />
        <Chart title="Incidents by severity" path="/analytics/incidents/severity" color="#e5484d" />
      </div>
      <div className="card"><h2>Ticket trend</h2>
        <select value={gran} onChange={(e) => setGran(e.target.value as 'day' | 'month')} aria-label="Granularity"><option value="day">Daily</option><option value="month">Monthly</option></select>
        {trend.data && <ResponsiveContainer width="100%" height={260}><LineChart data={trend.data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Line dataKey="total" stroke="#2f6fed" /></LineChart></ResponsiveContainer>}
      </div>
    </>
  );
}

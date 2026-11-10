export type Role = 'ADMIN' | 'MANAGER' | 'SUPPORT_AGENT' | 'EMPLOYEE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'ON_HOLD' | 'RESOLVED' | 'CLOSED';
export type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';

export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface User { id: number; email: string; fullName: string; role: Role; active: boolean }
export interface Ticket {
  id: number; title: string; description: string; category: string; priority: Priority; status: TicketStatus;
  reporterId: number; reporterName: string; assigneeId: number | null; assigneeName: string | null;
  assetId: number | null; assetName: string | null; dueAt: string | null; createdAt: string; resolvedAt: string | null; slaBreached: boolean;
}
export interface TicketComment { id: number; authorId: number; authorName: string; body: string; createdAt: string }
export interface Incident {
  id: number; title: string; description: string; severity: Priority; status: IncidentStatus; source: 'MANUAL' | 'MONITORING';
  assigneeId: number | null; assigneeName: string | null; ticketId: number | null; assetId: number | null; assetName: string | null;
  resolutionNotes: string | null; createdAt: string; resolvedAt: string | null;
}
export interface Asset { id: number; assetTag: string; name: string; assetType: string; status: string; ownerId: number | null; ownerName: string | null; location: string | null }
export interface MetricPoint { id: number; cpu: number; ram: number; disk: number; networkMbps: number; simulated: boolean; recordedAt: string }
export interface LatestMetric { assetId: number; assetName: string; assetType: string; latest: MetricPoint }
export interface MetricResult { assetId: number; cpu: number; ram: number; disk: number; networkMbps: number; simulated: boolean; recordedAt: string; anomaly: boolean }
export interface AnomalyEvent { id: number; assetId: number; detectionSource: string; score: number | null; reason: string; incidentId: number | null; createdAt: string }
export interface AppNotification { id: number; type: string; message: string; link: string | null; read: boolean; createdAt: string }
export interface LabelCount { label: string; total: number }

export const TICKET_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'ON_HOLD', 'RESOLVED'], IN_PROGRESS: ['ON_HOLD', 'RESOLVED', 'OPEN'],
  ON_HOLD: ['IN_PROGRESS', 'OPEN'], RESOLVED: ['CLOSED', 'IN_PROGRESS'], CLOSED: [],
};
export const INCIDENT_TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  OPEN: ['INVESTIGATING', 'RESOLVED'], INVESTIGATING: ['RESOLVED', 'OPEN'], RESOLVED: ['CLOSED', 'INVESTIGATING'], CLOSED: [],
};
export const isStaff = (r?: Role) => r === 'ADMIN' || r === 'MANAGER' || r === 'SUPPORT_AGENT';

import { lazy, Suspense, type ReactElement } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import Layout from './components/Layout';
import { Loading } from './components/ui';
import { isStaff } from './types';

const Login = lazy(() => import('./pages/Login'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Tickets = lazy(() => import('./pages/Tickets'));
const Incidents = lazy(() => import('./pages/Incidents'));
const Assets = lazy(() => import('./pages/Assets'));
const Monitoring = lazy(() => import('./pages/Monitoring'));
const AiInsights = lazy(() => import('./pages/AiInsights'));
const Reports = lazy(() => import('./pages/Reports'));
const Notifications = lazy(() => import('./pages/Notifications'));
const Users = lazy(() => import('./pages/Users'));
const Profile = lazy(() => import('./pages/Profile'));

function Protected({ children, staffOnly, managerOnly }: { children: ReactElement; staffOnly?: boolean; managerOnly?: boolean }) {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" replace />;
  if (staffOnly && !isStaff(user.role)) return <Navigate to="/tickets" replace />;
  if (managerOnly && user.role !== 'ADMIN' && user.role !== 'MANAGER') return <Navigate to="/tickets" replace />;
  return children;
}

export default function App() {
  const { user } = useAuth();
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
        <Route element={<Protected><Layout /></Protected>}>
          <Route index element={<Protected staffOnly><Dashboard /></Protected>} />
          <Route path="tickets" element={<Tickets />} />
          <Route path="incidents" element={<Protected staffOnly><Incidents /></Protected>} />
          <Route path="assets" element={<Protected staffOnly><Assets /></Protected>} />
          <Route path="monitoring" element={<Protected staffOnly><Monitoring /></Protected>} />
          <Route path="ai" element={<Protected staffOnly><AiInsights /></Protected>} />
          <Route path="reports" element={<Protected staffOnly><Reports /></Protected>} />
          <Route path="notifications" element={<Notifications />} />
          <Route path="users" element={<Protected managerOnly><Users /></Protected>} />
          <Route path="profile" element={<Profile />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

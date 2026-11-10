import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { isStaff } from '../types';

export default function Layout() {
  const { user, logout } = useAuth();
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const staff = isStaff(user?.role);
  const manager = user?.role === 'ADMIN' || user?.role === 'MANAGER';

  useEffect(() => {
    const load = () => api.get<{ count: number }>('/notifications/unread-count').then((r) => setUnread(r.data.count)).catch(() => undefined);
    load();
    const id = setInterval(load, 15000);
    return () => clearInterval(id);
  }, []);

  const links: [string, string, boolean][] = [
    ['/', 'Overview', staff], ['/tickets', 'Tickets', true], ['/incidents', 'Incidents', staff], ['/assets', 'Assets', staff],
    ['/monitoring', 'Monitoring', staff], ['/ai', 'AI Insights', staff], ['/reports', 'Reports', staff],
    ['/notifications', `Notifications${unread ? ` (${unread})` : ''}`, true], ['/users', 'Users', manager], ['/profile', 'Profile', true],
  ];
  return (
    <div className="shell">
      <button className="menu-toggle btn" onClick={() => setOpen(!open)} aria-label="Toggle menu">☰</button>
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand">EnterpriseOps <span>AI</span></div>
        <nav onClick={() => setOpen(false)}>
          {links.filter((l) => l[2]).map(([to, label]) => (
            <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>{label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <div>{user?.fullName}</div><small>{user?.role}</small>
          <button className="btn small" onClick={logout}>Log out</button>
        </div>
      </aside>
      <main className="content"><Outlet /></main>
    </div>
  );
}

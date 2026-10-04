import { UserButton, useUser } from '@clerk/clerk-react';
import { NavLink } from 'react-router-dom';

export default function Layout({ title, children }) {
  const { user } = useUser();
  const role = user?.publicMetadata?.role;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">Tenant Portal</div>
        <nav className="sidebar-nav">
          {role === 'OWNER' && (
            <NavLink to="/admin" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
              Owner dashboard
            </NavLink>
          )}
          {role === 'TENANT' && (
            <NavLink to="/tenant" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
              My dashboard
            </NavLink>
          )}
        </nav>
      </aside>

      <div className="main-area">
        <div className="topbar">
          <h1>{title}</h1>
          <UserButton />
        </div>
        <div className="content">{children}</div>
      </div>
    </div>
  );
}

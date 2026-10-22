import { useAuth } from 'react-oidc-context'
import { NavLink, Outlet } from 'react-router'
import { useProfile } from '../api/queries'
import { categoryLabels } from '../lib/labels'

function initials(name: string): string {
  return name
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

function NavItem({ to, children }: { to: string; children: string }) {
  return (
    <NavLink to={to} end className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
      {children}
    </NavLink>
  )
}

/** The side menu shows only what the signed-in person's roles allow. */
export function Layout() {
  const auth = useAuth()
  const { data: profile } = useProfile()
  const roles = profile?.roles ?? []
  const isAgent = roles.includes('agent')
  const isServiceManager = roles.includes('service-manager')
  const roleLine = isServiceManager
    ? 'Service manager'
    : isAgent && profile?.team
      ? `Agent · ${categoryLabels[profile.team]}`
      : `Staff${profile?.department ? ` · ${profile.department}` : ''}`

  return (
    <div className="app">
      <nav className="sidebar" aria-label="Main">
        <div className="brand">
          <div className="brand-mark" aria-hidden="true">
            SD
          </div>
          <span>Service Desk</span>
        </div>
        <div className="nav-group">
          <span className="nav-heading">Requests</span>
          <NavItem to="/">My requests</NavItem>
          <NavItem to="/new">New request</NavItem>
        </div>
        <div className="nav-group">
          <span className="nav-heading">Work</span>
          {isAgent && <NavItem to="/queue">Agent queue</NavItem>}
          <NavItem to="/approvals">Approvals</NavItem>
        </div>
        {isServiceManager && (
          <div className="nav-group">
            <span className="nav-heading">Insights</span>
            <NavItem to="/dashboard">Dashboard</NavItem>
          </div>
        )}
        <div className="user-card">
          <div className="avatar" aria-hidden="true">
            {profile ? initials(profile.fullName) : ''}
          </div>
          <div className="user-card-text">
            <span className="user-name">{profile?.fullName ?? auth.user?.profile.name}</span>
            <span className="muted small">{roleLine}</span>
          </div>
          <button type="button" className="link-button small" onClick={() => void auth.signoutRedirect()}>
            Sign out
          </button>
        </div>
      </nav>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}

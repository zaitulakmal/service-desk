import { useState } from 'react'
import { Link } from 'react-router'
import { useDashboard } from '../api/queries'
import { SlaIndicator } from '../components/Badges'
import { AlertIcon } from '../components/Icons'
import { ErrorMessage, Loading, PageHeader } from '../components/States'
import { formatDuration, timeAgo } from '../lib/format'
import { CategoryChart } from './CategoryChart'

const PERIODS = [
  { days: 7, label: 'Last 7 days' },
  { days: 30, label: 'Last 30 days' },
  { days: 90, label: 'Last 90 days' },
]

export function DashboardPage() {
  const [days, setDays] = useState(7)
  const dashboard = useDashboard(days)

  return (
    <div className="page page-wide">
      <PageHeader
        title="Service overview"
        subtitle="All support teams"
        actions={
          <select aria-label="Time range" value={days} onChange={(e) => setDays(Number(e.target.value))}>
            {PERIODS.map((p) => (
              <option key={p.days} value={p.days}>
                {p.label}
              </option>
            ))}
          </select>
        }
      />

      {dashboard.isPending ? (
        <Loading />
      ) : dashboard.isError ? (
        <ErrorMessage error={dashboard.error} />
      ) : (
        <>
          <div className="kpis">
            <div className="kpi">
              <span className="kpi-label">Open requests</span>
              <span className="kpi-value">{dashboard.data.openRequests}</span>
              <span className="muted small">Open or in progress now</span>
            </div>
            <div className="kpi">
              <span className="kpi-label">Awaiting approval</span>
              <span className="kpi-value">{dashboard.data.awaitingApproval}</span>
              <span className="muted small">SLA paused on these</span>
            </div>
            <div className="kpi">
              <span className="kpi-label">SLA breached</span>
              <span className="kpi-value">{dashboard.data.slaBreached}</span>
              {dashboard.data.slaBreached > 0 ? (
                <span className="sla sla-breached small">
                  <AlertIcon size={14} />
                  Needs attention
                </span>
              ) : (
                <span className="muted small">None in this period</span>
              )}
            </div>
            <div className="kpi">
              <span className="kpi-label">Avg. first response</span>
              <span className="kpi-value">
                {dashboard.data.averageFirstResponseSeconds !== null ? formatDuration(dashboard.data.averageFirstResponseSeconds) : '—'}
              </span>
              <span className="muted small">For requests raised in this period</span>
            </div>
          </div>

          <div className="two-columns">
            <div className="column-main">
              <CategoryChart data={dashboard.data.openByCategory} />
            </div>
            <section className="card column-side" aria-labelledby="attention-heading">
              <h2 id="attention-heading" className="card-title">
                Needs attention
              </h2>
              {dashboard.data.needsAttention.length === 0 && <p className="muted small">Every open request is on track.</p>}
              <div className="stack">
                {dashboard.data.needsAttention.map((t) => (
                  <Link key={t.id} to={`/tickets/${t.id}`} className="attention-item">
                    <span className="attention-top">
                      <span className="mono muted small">{t.reference}</span>
                      <SlaIndicator sla={t.sla} />
                    </span>
                    <span className="strong">{t.title}</span>
                    <span className="secondary small">{t.assignee?.fullName ?? 'Unassigned'}</span>
                  </Link>
                ))}
              </div>
            </section>
          </div>

          <section className="card" aria-labelledby="waiting-heading">
            <h2 id="waiting-heading" className="card-title">
              Approvals waiting longest
            </h2>
            <div className="table-scroll">
              <table className="table compact">
                <thead>
                  <tr>
                    <th scope="col">Request</th>
                    <th scope="col">Waiting on</th>
                    <th scope="col" className="numeric">
                      Waiting for
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.data.approvalsWaitingLongest.map((a) => (
                    <tr key={`${a.ticketId}-${a.waitingOn}`}>
                      <td>
                        <span className="mono muted small">{a.reference}</span>{' '}
                        <Link to={`/tickets/${a.ticketId}`} className="row-link">
                          {a.title}
                        </Link>
                      </td>
                      <td>{a.waitingOn}</td>
                      <td className="numeric strong">{timeAgo(a.since).replace(' ago', '')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {dashboard.data.approvalsWaitingLongest.length === 0 && <p className="empty">No approvals are waiting.</p>}
            </div>
          </section>
        </>
      )}
    </div>
  )
}

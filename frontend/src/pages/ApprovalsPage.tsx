import { Link } from 'react-router'
import { usePendingApprovals } from '../api/queries'
import { ErrorMessage, Loading, PageHeader } from '../components/States'
import { formatDateTime, timeAgo } from '../lib/format'
import { typeLabels } from '../lib/labels'

export function ApprovalsPage() {
  const approvals = usePendingApprovals()

  return (
    <div className="page">
      <PageHeader title="Approvals" subtitle="Requests waiting for your decision. The requester's clock is paused until you decide." />
      {approvals.isPending ? (
        <Loading />
      ) : approvals.isError ? (
        <ErrorMessage error={approvals.error} />
      ) : (
        <div className="table-card">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Request</th>
                <th scope="col">Requested by</th>
                <th scope="col">Your step</th>
                <th scope="col">Waiting</th>
              </tr>
            </thead>
            <tbody>
              {approvals.data.map((a) => (
                <tr key={a.ticketId}>
                  <td>
                    <div className="stack-tight">
                      <span className="mono muted small">{a.reference}</span>
                      <Link to={`/tickets/${a.ticketId}`} className="row-link">
                        {a.title}
                      </Link>
                      <span className="muted small">{typeLabels[a.type]}</span>
                    </div>
                  </td>
                  <td>{a.requester.fullName}</td>
                  <td className="secondary">{a.stepName}</td>
                  <td className="secondary" title={formatDateTime(a.pendingSince)}>
                    {timeAgo(a.pendingSince).replace(' ago', '')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {approvals.data.length === 0 && <p className="empty">Nothing is waiting for you.</p>}
        </div>
      )}
    </div>
  )
}

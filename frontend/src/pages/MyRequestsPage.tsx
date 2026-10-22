import { useState } from 'react'
import { Link } from 'react-router'
import { useMyRequestCounts, useMyRequests } from '../api/queries'
import type { TicketStatus } from '../api/types'
import { StatusPill } from '../components/Badges'
import { PlusIcon } from '../components/Icons'
import { ErrorMessage, Loading, PageHeader, Pagination } from '../components/States'
import { timeAgo } from '../lib/format'
import { categoryLabels, statusLabels } from '../lib/labels'

const FILTERS: (TicketStatus | null)[] = [null, 'OPEN', 'AWAITING_APPROVAL', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED']

export function MyRequestsPage() {
  const [status, setStatus] = useState<TicketStatus | null>(null)
  const [page, setPage] = useState(0)
  const requests = useMyRequests(status, page)
  const counts = useMyRequestCounts().data
  const total = counts ? Object.values(counts).reduce((sum, n) => sum + n, 0) : undefined

  return (
    <div className="page">
      <PageHeader
        title="My requests"
        subtitle="Everything you have asked IT, HR, Finance and Facilities for, in one place."
        actions={
          <Link to="/new" className="button button-primary">
            <PlusIcon />
            New request
          </Link>
        }
      />

      <div className="chips" role="group" aria-label="Filter by status">
        {FILTERS.filter((s) => s !== 'REJECTED' || (counts?.REJECTED ?? 0) > 0).map((s) => {
          const count = s ? counts?.[s] : total
          return (
            <button
              key={s ?? 'ALL'}
              type="button"
              className="chip"
              aria-pressed={status === s}
              onClick={() => {
                setStatus(s)
                setPage(0)
              }}
            >
              {s ? statusLabels[s] : 'All'}
              {count !== undefined && ` · ${count}`}
            </button>
          )
        })}
      </div>

      {requests.isPending ? (
        <Loading />
      ) : requests.isError ? (
        <ErrorMessage error={requests.error} />
      ) : (
        <>
          <div className="table-card">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">Request</th>
                  <th scope="col">Category</th>
                  <th scope="col">Status</th>
                  <th scope="col">Latest update</th>
                </tr>
              </thead>
              <tbody>
                {requests.data.content.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <div className="stack-tight">
                        <span className="mono muted small">{ticket.reference}</span>
                        <Link to={`/tickets/${ticket.id}`} className="row-link">
                          {ticket.title}
                        </Link>
                      </div>
                    </td>
                    <td className="secondary">{categoryLabels[ticket.category]}</td>
                    <td>
                      <StatusPill status={ticket.status} />
                    </td>
                    <td className="secondary">
                      {ticket.latestUpdate
                        ? `${ticket.latestUpdate.message} · ${timeAgo(ticket.latestUpdate.at)}`
                        : timeAgo(ticket.updatedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {requests.data.content.length === 0 && (
              <p className="empty">
                {status ? 'No requests with this status.' : 'You have not raised any requests yet.'}
              </p>
            )}
          </div>
          <Pagination page={page} totalPages={requests.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

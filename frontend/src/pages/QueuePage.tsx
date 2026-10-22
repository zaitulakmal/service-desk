import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAssignToMe, useProfile, useQueue, useQueueCounts } from '../api/queries'
import type { QueueView, TicketPriority } from '../api/types'
import { PriorityTag, SlaIndicator, StatusPill } from '../components/Badges'
import { ErrorMessage, Loading, PageHeader, Pagination } from '../components/States'
import { categoryLabels, priorityLabels } from '../lib/labels'

const VIEWS: { value: QueueView; label: string }[] = [
  { value: 'UNASSIGNED', label: 'Unassigned' },
  { value: 'MINE', label: 'Mine' },
  { value: 'ALL', label: 'All open' },
]

export function QueuePage() {
  const profile = useProfile().data
  const [view, setView] = useState<QueueView>('ALL')
  const [priority, setPriority] = useState<TicketPriority | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const queue = useQueue(view, priority, search, page)
  const counts = useQueueCounts().data
  const assign = useAssignToMe()

  // Wait until typing pauses before searching.
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchInput])

  const team = profile?.team ? categoryLabels[profile.team] : ''

  return (
    <div className="page page-wide">
      <PageHeader
        title={`${team} Support queue`}
        subtitle="Sorted by time left before the SLA is missed. Breached and at-risk requests come first."
      />

      <div className="toolbar">
        <div className="segmented-tabs" role="group" aria-label="Queue view">
          {VIEWS.map((v) => (
            <button
              key={v.value}
              type="button"
              aria-pressed={view === v.value}
              onClick={() => {
                setView(v.value)
                setPage(0)
              }}
            >
              {v.label}
              {counts && ` · ${counts[v.value]}`}
            </button>
          ))}
        </div>
        <div className="toolbar-filters">
          <input type="search" aria-label="Search requests" placeholder="Search by number or title" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
          <select aria-label="Priority" value={priority ?? ''} onChange={(e) => setPriority((e.target.value || null) as TicketPriority | null)}>
            <option value="">All priorities</option>
            {(['URGENT', 'HIGH', 'MEDIUM', 'LOW'] as TicketPriority[]).map((p) => (
              <option key={p} value={p}>
                {priorityLabels[p]}
              </option>
            ))}
          </select>
        </div>
      </div>

      {assign.isError && <ErrorMessage error={assign.error} />}

      {queue.isPending ? (
        <Loading />
      ) : queue.isError ? (
        <ErrorMessage error={queue.error} />
      ) : (
        <>
          <div className="table-card">
            <table className="table table-wide">
              <thead>
                <tr>
                  <th scope="col">Request</th>
                  <th scope="col">Requester</th>
                  <th scope="col">Priority</th>
                  <th scope="col">Status</th>
                  <th scope="col">Assignee</th>
                  <th scope="col">SLA</th>
                </tr>
              </thead>
              <tbody>
                {queue.data.content.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <div className="stack-tight">
                        <span className="mono muted small">{ticket.reference}</span>
                        <Link to={`/tickets/${ticket.id}`} className="row-link">
                          {ticket.title}
                        </Link>
                      </div>
                    </td>
                    <td>{ticket.requester.fullName}</td>
                    <td>
                      <PriorityTag priority={ticket.priority} />
                    </td>
                    <td>
                      <StatusPill status={ticket.status} />
                    </td>
                    <td>
                      {ticket.assignee ? (
                        <span className="nowrap">{ticket.assignee.fullName}</span>
                      ) : (
                        <button type="button" className="button button-outline button-small" disabled={assign.isPending} onClick={() => assign.mutate(ticket.id)}>
                          Assign to me
                        </button>
                      )}
                    </td>
                    <td className="nowrap">
                      <SlaIndicator sla={ticket.sla} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {queue.data.content.length === 0 && <p className="empty">Nothing here. Nice work.</p>}
          </div>
          <Pagination page={page} totalPages={queue.data.totalPages} onChange={setPage} />
        </>
      )}

      <p className="muted small">The SLA clock pauses while a request waits for approval, and restarts once it is approved.</p>
    </div>
  )
}

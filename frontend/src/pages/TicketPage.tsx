import { type FormEvent, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { useApi } from '../api/client'
import {
  useAddComment,
  useAssignToMe,
  useChangeStatus,
  useDecideApproval,
  useTicket,
  useUploadAttachment,
} from '../api/queries'
import type { ApprovalStepView, TicketDetail } from '../api/types'
import { PriorityTag, SlaIndicator, StatusPill } from '../components/Badges'
import { CheckIcon, ClockIcon, DownloadIcon, PaperclipIcon } from '../components/Icons'
import { ErrorMessage, Loading } from '../components/States'
import { formatBytes, formatDateTime, formatMoney, slaLabel } from '../lib/format'
import { categoryLabels, statusActions, statusLabels, typeLabels } from '../lib/labels'

function initials(name: string) {
  return name
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export function TicketPage() {
  const id = Number(useParams().id)
  const ticket = useTicket(id)
  const notice = (useLocation().state as { notice?: string } | null)?.notice

  if (ticket.isPending) return <Loading />
  if (ticket.isError) return <ErrorMessage error={ticket.error} />
  const t = ticket.data

  return (
    <div className="page page-wide">
      <Link to="/" className="back-link">
        ← Back
      </Link>
      <div className="ticket-header">
        <span className="mono muted">{t.reference}</span>
        <h1>{t.title}</h1>
        <div className="badges">
          <StatusPill status={t.status} />
          <PriorityTag priority={t.priority} />
          <span className="tag">
            {categoryLabels[t.category]} · {typeLabels[t.type]}
          </span>
        </div>
      </div>
      {notice && (
        <p className="notice notice-warning" role="status">
          {notice}
        </p>
      )}

      <div className="two-columns">
        <div className="column-main">
          {t.approvals.length > 0 && <ApprovalPanel ticket={t} />}
          <section className="card" aria-labelledby="description-heading">
            <h2 id="description-heading" className="card-title">
              Description
            </h2>
            <p className="prewrap">{t.description}</p>
            {t.purchaseAmount !== null && <p className="muted small">Estimated cost: {formatMoney(t.purchaseAmount)}</p>}
          </section>
          <Conversation ticket={t} />
        </div>

        <aside className="column-side">
          <AgentActions ticket={t} />
          <section className="card" aria-labelledby="details-heading">
            <h2 id="details-heading" className="card-title">
              Details
            </h2>
            <dl className="details">
              <div>
                <dt>Requester</dt>
                <dd>{t.requester.fullName}</dd>
              </div>
              <div>
                <dt>Department</dt>
                <dd>{t.requesterDepartment ?? '—'}</dd>
              </div>
              <div>
                <dt>Assignee</dt>
                <dd>{t.assignee?.fullName ?? 'Unassigned'}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{formatDateTime(t.createdAt)}</dd>
              </div>
              <div className="span-2">
                <dt>SLA</dt>
                <dd>
                  <SlaIndicator sla={t.sla} />
                  {t.sla.dueAt && ['ON_TRACK', 'AT_RISK', 'BREACHED'].includes(t.sla.state) && (
                    <span className="muted small"> · due {formatDateTime(t.sla.dueAt)}</span>
                  )}
                </dd>
              </div>
            </dl>
          </section>
          <Attachments ticket={t} />
          <section className="card" aria-labelledby="activity-heading">
            <h2 id="activity-heading" className="card-title">
              Activity log
            </h2>
            <ul className="activity">
              {t.activity.map((a, i) => (
                <li key={i}>
                  <span>{a.message}</span>
                  <span className="muted small">{formatDateTime(a.at)}</span>
                </li>
              ))}
            </ul>
          </section>
        </aside>
      </div>
    </div>
  )
}

function StepIcon({ step }: { step: ApprovalStepView }) {
  switch (step.status) {
    case 'APPROVED':
      return (
        <span className="step-icon step-done" role="img" aria-label="Approved">
          <CheckIcon size={14} />
        </span>
      )
    case 'PENDING':
      return (
        <span className="step-icon step-pending" role="img" aria-label="Waiting for a decision">
          <ClockIcon size={14} />
        </span>
      )
    case 'REJECTED':
      return (
        <span className="step-icon step-rejected" role="img" aria-label="Rejected">
          ✕
        </span>
      )
    default:
      return <span className="step-icon step-waiting" role="img" aria-label="Not started" />
  }
}

function stepMeta(step: ApprovalStepView): string {
  if (step.status === 'APPROVED' && step.decidedAt) return `${step.decidedBy} · Approved ${formatDateTime(step.decidedAt)}`
  if (step.status === 'REJECTED' && step.decidedAt) return `${step.decidedBy} · Rejected ${formatDateTime(step.decidedAt)}`
  if (step.status === 'PENDING' && step.pendingSince) return `${step.approverName} · Waiting since ${formatDateTime(step.pendingSince)}`
  return `${step.approverName} · After the step above`
}

function ApprovalPanel({ ticket }: { ticket: TicketDetail }) {
  const decide = useDecideApproval(ticket.id)
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const pending = ticket.approvals.find((s) => s.status === 'PENDING')

  function reject(event: FormEvent) {
    event.preventDefault()
    decide.mutate({ decision: 'REJECT', comment: reason }, { onSuccess: () => setRejecting(false) })
  }

  return (
    <section className="card" aria-labelledby="approval-heading">
      <h2 id="approval-heading" className="card-title">
        Approval
      </h2>
      <ol className="stepper">
        <li className="step">
          <span className="step-icon step-done" role="img" aria-label="Done">
            <CheckIcon size={14} />
          </span>
          <div className="step-body">
            <strong>Requested</strong>
            <span className="muted small">
              {ticket.requester.fullName} · {formatDateTime(ticket.createdAt)}
            </span>
          </div>
        </li>
        {ticket.approvals.map((step) => (
          <li key={step.id} className="step">
            <StepIcon step={step} />
            <div className="step-body">
              <strong>{step.name}</strong>
              <span className="muted small">{stepMeta(step)}</span>
              {step.comment && <span className="quote">“{step.comment}”</span>}
            </div>
          </li>
        ))}
      </ol>

      {ticket.permissions.canDecideApproval && pending && (
        <div className="decision">
          <p>
            <strong>Your approval is needed.</strong> {ticket.requester.fullName} is asking for “{ticket.title}”.
          </p>
          {rejecting ? (
            <form onSubmit={reject} className="stack">
              <label htmlFor="reason">Why are you rejecting it? {ticket.requester.fullName.split(' ')[0]} will see this.</label>
              <textarea id="reason" rows={3} value={reason} onChange={(e) => setReason(e.target.value)} required />
              <div className="form-actions">
                <button type="submit" className="button button-danger" disabled={!reason.trim() || decide.isPending}>
                  Reject request
                </button>
                <button type="button" className="button button-quiet" onClick={() => setRejecting(false)}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <div className="form-actions">
              <button type="button" className="button button-primary" disabled={decide.isPending} onClick={() => decide.mutate({ decision: 'APPROVE', comment: null })}>
                Approve
              </button>
              <button type="button" className="button button-outline-danger" onClick={() => setRejecting(true)}>
                Reject
              </button>
            </div>
          )}
          {decide.isError && <ErrorMessage error={decide.error} />}
        </div>
      )}
    </section>
  )
}

function Conversation({ ticket }: { ticket: TicketDetail }) {
  const addComment = useAddComment(ticket.id)
  const [reply, setReply] = useState('')

  function send(event: FormEvent) {
    event.preventDefault()
    addComment.mutate(reply, { onSuccess: () => setReply('') })
  }

  return (
    <section className="card" aria-labelledby="conversation-heading">
      <h2 id="conversation-heading" className="card-title">
        Conversation
      </h2>
      {ticket.comments.length === 0 && <p className="muted">No messages yet.</p>}
      <div className="stack">
        {ticket.comments.map((c) => (
          <div key={c.id} className="message">
            <div className="avatar" aria-hidden="true">
              {initials(c.author.fullName)}
            </div>
            <div className="message-body">
              <div className="message-meta">
                <strong>{c.author.fullName}</strong>
                <span className="muted small">{formatDateTime(c.createdAt)}</span>
              </div>
              <p className="prewrap">{c.body}</p>
            </div>
          </div>
        ))}
      </div>
      {ticket.permissions.canComment && (
        <form className="reply" onSubmit={send}>
          <label htmlFor="reply">Write a reply</label>
          <textarea id="reply" rows={3} value={reply} onChange={(e) => setReply(e.target.value)} maxLength={5000} />
          <div className="form-actions">
            <button type="submit" className="button button-dark" disabled={!reply.trim() || addComment.isPending}>
              Send reply
            </button>
          </div>
          {addComment.isError && <ErrorMessage error={addComment.error} />}
        </form>
      )}
    </section>
  )
}

function AgentActions({ ticket }: { ticket: TicketDetail }) {
  const assign = useAssignToMe()
  const changeStatus = useChangeStatus(ticket.id)
  const { canAssignToMe, allowedStatuses } = ticket.permissions
  if (!canAssignToMe && allowedStatuses.length === 0) return null

  return (
    <section className="card" aria-labelledby="actions-heading">
      <h2 id="actions-heading" className="card-title">
        Work on this request
      </h2>
      <div className="form-actions">
        {canAssignToMe && (
          <button type="button" className="button button-primary" disabled={assign.isPending} onClick={() => assign.mutate(ticket.id)}>
            Assign to me
          </button>
        )}
        {allowedStatuses.map((status) => (
          <button key={status} type="button" className={status === 'CLOSED' ? 'button button-quiet' : 'button button-outline'} disabled={changeStatus.isPending} onClick={() => changeStatus.mutate(status)}>
            {ticket.status === 'RESOLVED' && status === 'IN_PROGRESS' ? 'Reopen' : (statusActions[status] ?? statusLabels[status])}
          </button>
        ))}
      </div>
      {ticket.status === 'AWAITING_APPROVAL' && <p className="muted small">You can take it now; work starts once it is approved ({slaLabel(ticket.sla).toLowerCase()}).</p>}
      {(assign.isError || changeStatus.isError) && <ErrorMessage error={assign.error ?? changeStatus.error} />}
    </section>
  )
}

function Attachments({ ticket }: { ticket: TicketDetail }) {
  const api = useApi()
  const upload = useUploadAttachment(ticket.id)
  const [downloadError, setDownloadError] = useState<unknown>(null)

  return (
    <section className="card" aria-labelledby="attachments-heading">
      <h2 id="attachments-heading" className="card-title">
        Attachments
      </h2>
      {ticket.attachments.length === 0 && <p className="muted small">No files yet.</p>}
      <ul className="attachments">
        {ticket.attachments.map((a) => (
          <li key={a.id}>
            <button
              type="button"
              className="link-button attachment"
              onClick={() =>
                api.download(`/api/tickets/${ticket.id}/attachments/${a.id}`, a.fileName).catch(setDownloadError)
              }
            >
              <DownloadIcon />
              {a.fileName}
            </button>
            <span className="muted small">
              {formatBytes(a.sizeBytes)} · {a.uploadedBy}
            </span>
          </li>
        ))}
      </ul>
      {ticket.permissions.canComment && (
        <label className="button button-outline button-small upload">
          <PaperclipIcon />
          {upload.isPending ? 'Uploading…' : 'Attach a file'}
          <input
            type="file"
            className="visually-hidden"
            accept="image/*,.pdf,.txt,.csv,.docx,.xlsx,.pptx"
            disabled={upload.isPending}
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (file) upload.mutate(file)
              e.target.value = ''
            }}
          />
        </label>
      )}
      {upload.isError && <ErrorMessage error={upload.error} />}
      {downloadError !== null && <ErrorMessage error={downloadError} />}
    </section>
  )
}

import { useQueryClient } from '@tanstack/react-query'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError, useApi } from '../api/client'
import { useCreateTicket, useTicketOptions } from '../api/queries'
import type { TicketCategory, TicketDetail, TicketPriority, TicketType } from '../api/types'
import { InfoIcon } from '../components/Icons'
import { ErrorMessage, Loading, PageHeader } from '../components/States'
import { formatMoney, formatSlaTarget } from '../lib/format'
import { categoryLabels, priorityLabels } from '../lib/labels'

export function NewRequestPage() {
  const options = useTicketOptions()
  const createTicket = useCreateTicket()
  const api = useApi()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [type, setType] = useState<TicketType>('INCIDENT')
  const [category, setCategory] = useState<TicketCategory | ''>('')
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM')
  const [amount, setAmount] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [uploading, setUploading] = useState(false)

  if (options.isPending) return <Loading />
  if (options.isError) return <ErrorMessage error={options.error} />

  const threshold = options.data.purchaseApprovalThreshold
  const slaTarget = options.data.priorities.find((p) => p.value === priority)?.slaTargetSeconds ?? 0
  const fieldErrors = createTicket.error instanceof ApiError ? createTicket.error.fieldErrors : {}
  const amountNumber = Number(amount)
  const approvalNote =
    type === 'ACCESS'
      ? 'Access requests need approval from your manager, then IT Security. We send it to them as soon as you submit.'
      : type === 'PURCHASE' && amountNumber > threshold
        ? `Purchases above ${formatMoney(threshold)} need approval from your manager first.`
        : null

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!category) return
    let ticket: TicketDetail
    try {
      ticket = await createTicket.mutateAsync({
        title,
        description,
        type,
        category,
        priority,
        purchaseAmount: type === 'PURCHASE' && amount ? amountNumber : null,
      })
    } catch {
      return // shown next to the fields
    }
    // Files go up one by one once the ticket exists; each upload returns the updated ticket.
    setUploading(true)
    const failed: string[] = []
    for (const file of files) {
      try {
        const form = new FormData()
        form.append('file', file)
        ticket = await api.post<TicketDetail>(`/api/tickets/${ticket.id}/attachments`, form)
      } catch {
        failed.push(file.name)
      }
    }
    queryClient.setQueryData(['ticket', ticket.id], ticket)
    setUploading(false)
    navigate(`/tickets/${ticket.id}`, {
      state: failed.length ? { notice: `Your request was created, but these files could not be attached: ${failed.join(', ')}` } : undefined,
    })
  }

  const busy = createTicket.isPending || uploading

  return (
    <div className="page">
      <Link to="/" className="back-link">
        ← Back to my requests
      </Link>
      <PageHeader title="New request" subtitle="Tell us what you need. We route it to the right team and track it until it is done." />

      <div className="two-columns">
        <form className="card form column-main" onSubmit={(e) => void submit(e)} noValidate>
          <div className="field">
            <label htmlFor="title">What do you need help with?</label>
            <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Short summary, e.g. Cannot log in to VPN" required maxLength={200} aria-invalid={!!fieldErrors.title} />
            {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
          </div>

          <div className="field">
            <label htmlFor="description">Details</label>
            <textarea id="description" rows={5} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="What happened, since when, and what you have already tried" required maxLength={5000} aria-invalid={!!fieldErrors.description} />
            {fieldErrors.description && <span className="field-error">{fieldErrors.description}</span>}
          </div>

          <div className="field-grid">
            <div className="field">
              <label htmlFor="type">Type of request</label>
              <select id="type" value={type} onChange={(e) => setType(e.target.value as TicketType)}>
                {options.data.types.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="category">Category</label>
              <select id="category" value={category} onChange={(e) => setCategory(e.target.value as TicketCategory)} required aria-invalid={!!fieldErrors.category}>
                <option value="">Choose a category</option>
                {options.data.categories.map((c) => (
                  <option key={c} value={c}>
                    {categoryLabels[c]}
                  </option>
                ))}
              </select>
              {fieldErrors.category && <span className="field-error">{fieldErrors.category}</span>}
            </div>
          </div>

          {type === 'PURCHASE' && (
            <div className="field">
              <label htmlFor="amount">Estimated cost (RM)</label>
              <input id="amount" type="number" min="0.01" step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} aria-invalid={!!fieldErrors.purchaseAmount} />
              {fieldErrors.purchaseAmount && <span className="field-error">Estimated cost {fieldErrors.purchaseAmount}</span>}
            </div>
          )}

          <fieldset className="field">
            <legend>Priority</legend>
            <div className="segmented">
              {options.data.priorities.map((p) => (
                <label key={p.value} className="segment">
                  <input type="radio" name="priority" value={p.value} checked={priority === p.value} onChange={() => setPriority(p.value)} />
                  {priorityLabels[p.value]}
                </label>
              ))}
            </div>
            <p className="hint">
              Target resolution: {formatSlaTarget(slaTarget)}.
              {priority === 'URGENT' && ' Use only when you cannot work at all.'}
            </p>
          </fieldset>

          <div className="field">
            <label htmlFor="files">Attachments</label>
            <input id="files" type="file" multiple className="file-input" onChange={(e) => setFiles(Array.from(e.target.files ?? []))} accept="image/*,.pdf,.txt,.csv,.docx,.xlsx,.pptx" />
            <span className="hint">Screenshots, PDFs, text or Office files, up to 10 MB each.</span>
          </div>

          {approvalNote && (
            <div className="notice notice-warning">
              <InfoIcon size={18} />
              <p>{approvalNote}</p>
            </div>
          )}

          {createTicket.isError && !(createTicket.error instanceof ApiError && Object.keys(fieldErrors).length) && (
            <ErrorMessage error={createTicket.error} />
          )}

          <div className="form-actions">
            <button type="submit" className="button button-primary" disabled={busy || !title.trim() || !description.trim() || !category}>
              {uploading ? 'Uploading files…' : createTicket.isPending ? 'Submitting…' : 'Submit request'}
            </button>
            <Link to="/" className="button button-quiet">
              Cancel
            </Link>
          </div>
        </form>

        <aside className="column-side">
          <section className="card">
            <h2 className="card-title">What happens next</h2>
            <ol className="steps-list">
              <li>We email you a receipt with your request number.</li>
              {approvalNote && <li>Your approvers get an email. The clock waits while they decide.</li>}
              <li>An agent from the {category ? categoryLabels[category] : 'right'} team picks it up.</li>
              <li>
                We aim to resolve it {formatSlaTarget(slaTarget)}. You can follow every step on the request page.
              </li>
            </ol>
          </section>
        </aside>
      </div>
    </div>
  )
}

import type { Sla, TicketPriority, TicketStatus } from '../api/types'
import { slaLabel } from '../lib/format'
import { priorityLabels, statusLabels } from '../lib/labels'
import { AlertIcon, CheckIcon, ClockIcon, PauseIcon } from './Icons'

export function StatusPill({ status }: { status: TicketStatus }) {
  return <span className={`pill status-${status.toLowerCase()}`}>{statusLabels[status]}</span>
}

export function PriorityTag({ priority }: { priority: TicketPriority }) {
  return <span className={`tag priority-${priority.toLowerCase()}`}>{priorityLabels[priority]}</span>
}

/** SLA status with an icon and words, so it never depends on colour alone. */
export function SlaIndicator({ sla }: { sla: Sla }) {
  const tone = {
    BREACHED: 'sla-breached',
    AT_RISK: 'sla-risk',
    ON_TRACK: 'sla-ok',
    PAUSED: 'sla-muted',
    MET: 'sla-ok',
    MISSED: 'sla-breached',
    NONE: 'sla-muted',
  }[sla.state]
  const icon =
    sla.state === 'BREACHED' || sla.state === 'MISSED' ? (
      <AlertIcon />
    ) : sla.state === 'PAUSED' ? (
      <PauseIcon />
    ) : sla.state === 'MET' ? (
      <CheckIcon />
    ) : sla.state === 'NONE' ? null : (
      <ClockIcon />
    )
  return (
    <span className={`sla ${tone}`}>
      {icon}
      {slaLabel(sla)}
    </span>
  )
}

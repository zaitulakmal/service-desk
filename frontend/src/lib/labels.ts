import type { TicketCategory, TicketPriority, TicketStatus, TicketType } from '../api/types'

export const statusLabels: Record<TicketStatus, string> = {
  AWAITING_APPROVAL: 'Awaiting approval',
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  REJECTED: 'Rejected',
}

export const categoryLabels: Record<TicketCategory, string> = {
  IT: 'IT',
  HR: 'HR',
  FINANCE: 'Finance',
  FACILITIES: 'Facilities',
}

export const priorityLabels: Record<TicketPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
}

export const typeLabels: Record<TicketType, string> = {
  INCIDENT: 'Something is broken',
  SERVICE_REQUEST: 'Ask for something',
  ACCESS: 'Access to a system',
  PURCHASE: 'Buy something',
}

/** Words on the buttons that move a ticket to each status. */
export const statusActions: Partial<Record<TicketStatus, string>> = {
  IN_PROGRESS: 'Start work',
  RESOLVED: 'Mark resolved',
  CLOSED: 'Close',
}

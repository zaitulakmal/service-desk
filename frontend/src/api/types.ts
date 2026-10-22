// Mirrors the JSON returned by the Spring Boot API.

export type TicketStatus = 'AWAITING_APPROVAL' | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED'
export type TicketCategory = 'IT' | 'HR' | 'FINANCE' | 'FACILITIES'
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type TicketType = 'INCIDENT' | 'SERVICE_REQUEST' | 'ACCESS' | 'PURCHASE'
export type SlaState = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'PAUSED' | 'MET' | 'MISSED' | 'NONE'
export type ApprovalStatus = 'WAITING' | 'PENDING' | 'APPROVED' | 'REJECTED'
export type QueueView = 'UNASSIGNED' | 'MINE' | 'ALL'

export interface UserSummary {
  id: string
  fullName: string
  email: string
  jobTitle: string | null
}

export interface Profile {
  id: string
  fullName: string
  email: string
  department: string | null
  jobTitle: string | null
  team: TicketCategory | null
  roles: string[]
  managerName: string | null
}

export interface Sla {
  state: SlaState
  dueAt: string | null
  /** Negative once breached; while paused, the time left when the clock restarts. */
  remainingSeconds: number | null
  targetSeconds: number
  breachedAt: string | null
}

export interface TicketSummary {
  id: number
  reference: string
  title: string
  type: TicketType
  category: TicketCategory
  priority: TicketPriority
  status: TicketStatus
  requester: UserSummary
  assignee: UserSummary | null
  sla: Sla
  latestUpdate: { message: string; at: string } | null
  createdAt: string
  updatedAt: string
}

export interface ApprovalStepView {
  id: number
  order: number
  name: string
  approverName: string
  status: ApprovalStatus
  pendingSince: string | null
  decidedBy: string | null
  decidedAt: string | null
  comment: string | null
}

export interface CommentView {
  id: number
  author: UserSummary
  body: string
  createdAt: string
}

export interface AttachmentView {
  id: number
  fileName: string
  contentType: string
  sizeBytes: number
  uploadedBy: string
  createdAt: string
}

export interface ActivityView {
  type: string
  message: string
  at: string
}

export interface TicketDetail {
  id: number
  reference: string
  title: string
  description: string
  type: TicketType
  category: TicketCategory
  priority: TicketPriority
  status: TicketStatus
  purchaseAmount: number | null
  requester: UserSummary
  requesterDepartment: string | null
  assignee: UserSummary | null
  sla: Sla
  createdAt: string
  updatedAt: string
  firstResponseAt: string | null
  resolvedAt: string | null
  approvals: ApprovalStepView[]
  comments: CommentView[]
  attachments: AttachmentView[]
  activity: ActivityView[]
  permissions: {
    canAssignToMe: boolean
    allowedStatuses: TicketStatus[]
    canComment: boolean
    canDecideApproval: boolean
  }
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TicketOptions {
  types: { value: TicketType; label: string }[]
  categories: TicketCategory[]
  priorities: { value: TicketPriority; slaTargetSeconds: number }[]
  purchaseApprovalThreshold: number
}

export interface NewTicket {
  title: string
  description: string
  type: TicketType
  category: TicketCategory
  priority: TicketPriority
  purchaseAmount: number | null
}

export interface PendingApproval {
  ticketId: number
  reference: string
  title: string
  type: TicketType
  requester: UserSummary
  stepName: string
  pendingSince: string
}

export interface Dashboard {
  periodDays: number
  openRequests: number
  awaitingApproval: number
  slaBreached: number
  averageFirstResponseSeconds: number | null
  openByCategory: { category: TicketCategory; count: number }[]
  needsAttention: TicketSummary[]
  approvalsWaitingLongest: { ticketId: number; reference: string; title: string; waitingOn: string; since: string }[]
}

import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useApi } from './client'
import type {
  Dashboard,
  NewTicket,
  Page,
  PendingApproval,
  Profile,
  QueueView,
  TicketDetail,
  TicketOptions,
  TicketPriority,
  TicketStatus,
  TicketSummary,
} from './types'

export function useProfile() {
  const api = useApi()
  return useQuery({ queryKey: ['me'], queryFn: ({ signal }) => api.get<Profile>('/api/me', signal), staleTime: 5 * 60_000 })
}

export function useTicketOptions() {
  const api = useApi()
  return useQuery({
    queryKey: ['ticket-options'],
    queryFn: ({ signal }) => api.get<TicketOptions>('/api/tickets/options', signal),
    staleTime: Infinity,
  })
}

export function useMyRequests(status: TicketStatus | null, page: number) {
  const api = useApi()
  const params = new URLSearchParams({ page: String(page) })
  if (status) params.set('status', status)
  return useQuery({
    queryKey: ['my-requests', status, page],
    queryFn: ({ signal }) => api.get<Page<TicketSummary>>(`/api/tickets/mine?${params}`, signal),
    placeholderData: keepPreviousData,
  })
}

export function useMyRequestCounts() {
  const api = useApi()
  return useQuery({
    queryKey: ['my-request-counts'],
    queryFn: ({ signal }) => api.get<Record<TicketStatus, number>>('/api/tickets/mine/counts', signal),
  })
}

export function useQueue(view: QueueView, priority: TicketPriority | null, search: string, page: number) {
  const api = useApi()
  const params = new URLSearchParams({ view, page: String(page) })
  if (priority) params.set('priority', priority)
  if (search) params.set('q', search)
  return useQuery({
    queryKey: ['queue', view, priority, search, page],
    queryFn: ({ signal }) => api.get<Page<TicketSummary>>(`/api/queue?${params}`, signal),
    placeholderData: keepPreviousData,
    refetchInterval: 30_000,
  })
}

export function useQueueCounts() {
  const api = useApi()
  return useQuery({
    queryKey: ['queue-counts'],
    queryFn: ({ signal }) => api.get<Record<QueueView, number>>('/api/queue/counts', signal),
    refetchInterval: 30_000,
  })
}

export function useTicket(id: number) {
  const api = useApi()
  return useQuery({ queryKey: ['ticket', id], queryFn: ({ signal }) => api.get<TicketDetail>(`/api/tickets/${id}`, signal) })
}

export function usePendingApprovals() {
  const api = useApi()
  return useQuery({
    queryKey: ['pending-approvals'],
    queryFn: ({ signal }) => api.get<PendingApproval[]>('/api/approvals/pending', signal),
  })
}

export function useDashboard(days: number) {
  const api = useApi()
  return useQuery({
    queryKey: ['dashboard', days],
    queryFn: ({ signal }) => api.get<Dashboard>(`/api/dashboard?days=${days}`, signal),
    refetchInterval: 60_000,
  })
}

/**
 * After any change, show the new ticket straight away and refresh every list it appears in.
 *
 * Lists are cancelled before they are refreshed. Otherwise a list request that started
 * before the change was saved (e.g. the user was typing a search) would be reused, and
 * the list would keep showing the old state.
 */
function useTicketChanged() {
  const queryClient = useQueryClient()
  return async (ticket: TicketDetail) => {
    queryClient.setQueryData(['ticket', ticket.id], ticket)
    const lists = ['my-requests', 'my-request-counts', 'queue', 'queue-counts', 'pending-approvals', 'dashboard']
    await Promise.all(lists.map((key) => queryClient.cancelQueries({ queryKey: [key] })))
    await Promise.all(lists.map((key) => queryClient.invalidateQueries({ queryKey: [key] })))
  }
}

export function useCreateTicket() {
  const api = useApi()
  const ticketChanged = useTicketChanged()
  return useMutation({
    mutationFn: (ticket: NewTicket) => api.post<TicketDetail>('/api/tickets', ticket),
    onSuccess: ticketChanged,
  })
}

export function useUploadAttachment(ticketId: number) {
  const api = useApi()
  const ticketChanged = useTicketChanged()
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return api.post<TicketDetail>(`/api/tickets/${ticketId}/attachments`, form)
    },
    onSuccess: ticketChanged,
  })
}

export function useAssignToMe() {
  const api = useApi()
  const ticketChanged = useTicketChanged()
  return useMutation({
    mutationFn: (ticketId: number) => api.post<TicketDetail>(`/api/tickets/${ticketId}/assign`),
    onSuccess: ticketChanged,
  })
}

export function useChangeStatus(ticketId: number) {
  const api = useApi()
  const ticketChanged = useTicketChanged()
  return useMutation({
    mutationFn: (status: TicketStatus) => api.patch<TicketDetail>(`/api/tickets/${ticketId}/status`, { status }),
    onSuccess: ticketChanged,
  })
}

export function useAddComment(ticketId: number) {
  const api = useApi()
  const ticketChanged = useTicketChanged()
  return useMutation({
    mutationFn: (body: string) => api.post<TicketDetail>(`/api/tickets/${ticketId}/comments`, { body }),
    onSuccess: ticketChanged,
  })
}

export function useDecideApproval(ticketId: number) {
  const api = useApi()
  const ticketChanged = useTicketChanged()
  return useMutation({
    mutationFn: (decision: { decision: 'APPROVE' | 'REJECT'; comment: string | null }) =>
      api.post<TicketDetail>(`/api/tickets/${ticketId}/approval`, decision),
    onSuccess: ticketChanged,
  })
}

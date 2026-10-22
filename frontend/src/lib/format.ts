import type { Sla } from '../api/types'

/** 2400 → "40m", 8100 → "2h 15m", 97200 → "1d 3h". */
export function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(Math.abs(totalSeconds) / 60)
  if (minutes < 1) return 'under 1m'
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const restMinutes = minutes % 60
  if (hours < 24) return restMinutes ? `${hours}h ${restMinutes}m` : `${hours}h`
  const days = Math.floor(hours / 24)
  const restHours = hours % 24
  return restHours ? `${days}d ${restHours}h` : `${days}d`
}

/** "just now", "2h ago", "3d ago". */
export function timeAgo(iso: string, now: number = Date.now()): string {
  const seconds = (now - new Date(iso).getTime()) / 1000
  return seconds < 60 ? 'just now' : `${formatDuration(seconds)} ago`
}

const dateTime = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})

/** "19 Oct, 09:12" in the viewer's time zone. */
export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso))
}

/** The SLA as people read it: "2h 15m left", "Breached 25m ago", "Paused for approval". */
export function slaLabel(sla: Sla): string {
  switch (sla.state) {
    case 'ON_TRACK':
    case 'AT_RISK':
      return `${formatDuration(sla.remainingSeconds ?? 0)} left`
    case 'BREACHED':
      return `Breached ${formatDuration(sla.remainingSeconds ?? 0)} ago`
    case 'PAUSED':
      return 'Paused for approval'
    case 'MET':
      return 'Met'
    case 'MISSED':
      return 'Missed'
    case 'NONE':
      return '—'
  }
}

/** 28800 → "within 8 hours", 259200 → "within 3 days". */
export function formatSlaTarget(seconds: number): string {
  const hours = seconds / 3600
  if (hours < 24) return `within ${hours} ${hours === 1 ? 'hour' : 'hours'}`
  const days = hours / 24
  return `within ${days} ${days === 1 ? 'day' : 'days'}`
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const ringgit = new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR' })

export function formatMoney(amount: number): string {
  return ringgit.format(amount)
}

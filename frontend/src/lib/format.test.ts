import { describe, expect, it } from 'vitest'
import type { Sla } from '../api/types'
import { formatBytes, formatDuration, formatSlaTarget, slaLabel, timeAgo } from './format'

function sla(state: Sla['state'], remainingSeconds: number | null = null): Sla {
  return { state, remainingSeconds, dueAt: null, targetSeconds: 28800, breachedAt: null }
}

describe('formatDuration', () => {
  it('uses the two largest units', () => {
    expect(formatDuration(30)).toBe('under 1m')
    expect(formatDuration(40 * 60)).toBe('40m')
    expect(formatDuration(2 * 3600 + 15 * 60)).toBe('2h 15m')
    expect(formatDuration(3 * 3600)).toBe('3h')
    expect(formatDuration(27 * 3600)).toBe('1d 3h')
  })

  it('ignores the sign so breached times read naturally', () => {
    expect(formatDuration(-25 * 60)).toBe('25m')
  })
})

describe('slaLabel', () => {
  it('describes each SLA state in words', () => {
    expect(slaLabel(sla('ON_TRACK', 8100))).toBe('2h 15m left')
    expect(slaLabel(sla('AT_RISK', 2400))).toBe('40m left')
    expect(slaLabel(sla('BREACHED', -1500))).toBe('Breached 25m ago')
    expect(slaLabel(sla('PAUSED', 28800))).toBe('Paused for approval')
    expect(slaLabel(sla('MET'))).toBe('Met')
  })
})

describe('timeAgo', () => {
  it('rounds to the nearest useful unit', () => {
    const now = Date.parse('2026-10-20T12:00:00Z')
    expect(timeAgo('2026-10-20T11:59:30Z', now)).toBe('just now')
    expect(timeAgo('2026-10-20T10:00:00Z', now)).toBe('2h ago')
    expect(timeAgo('2026-10-17T12:00:00Z', now)).toBe('3d ago')
  })
})

describe('formatSlaTarget', () => {
  it('reads as a promise', () => {
    expect(formatSlaTarget(4 * 3600)).toBe('within 4 hours')
    expect(formatSlaTarget(24 * 3600)).toBe('within 1 day')
    expect(formatSlaTarget(5 * 86400)).toBe('within 5 days')
  })
})

describe('formatBytes', () => {
  it('picks a readable unit', () => {
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(2048)).toBe('2.0 KB')
    expect(formatBytes(3 * 1024 * 1024)).toBe('3.0 MB')
  })
})

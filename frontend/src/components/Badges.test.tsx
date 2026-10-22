import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SlaIndicator, StatusPill } from './Badges'

describe('SlaIndicator', () => {
  it('never relies on colour alone: a breach says so in words', () => {
    render(<SlaIndicator sla={{ state: 'BREACHED', remainingSeconds: -1500, dueAt: null, targetSeconds: 14400, breachedAt: null }} />)

    const indicator = screen.getByText('Breached 25m ago')
    expect(indicator).toHaveClass('sla-breached')
  })
})

describe('StatusPill', () => {
  it('shows the human label', () => {
    render(<StatusPill status="AWAITING_APPROVAL" />)

    expect(screen.getByText('Awaiting approval')).toHaveClass('pill', 'status-awaiting_approval')
  })
})

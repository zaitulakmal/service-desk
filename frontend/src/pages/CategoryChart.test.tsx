import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { CategoryChart } from './CategoryChart'

const data = [
  { category: 'IT' as const, count: 6 },
  { category: 'HR' as const, count: 2 },
  { category: 'FINANCE' as const, count: 0 },
  { category: 'FACILITIES' as const, count: 0 },
]

describe('CategoryChart', () => {
  it('labels every bar so the numbers are readable without hovering', () => {
    render(<CategoryChart data={data} />)

    expect(screen.getByRole('button', { name: 'IT: 6 open requests' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'HR: 2 open requests' })).toBeInTheDocument()
  })

  it('shows the share on hover and on keyboard focus', async () => {
    const user = userEvent.setup()
    render(<CategoryChart data={data} />)

    await user.hover(screen.getByRole('button', { name: 'IT: 6 open requests' }))
    expect(screen.getByRole('tooltip')).toHaveTextContent('IT · 75% of total')

    const itBar = screen.getByRole('button', { name: 'IT: 6 open requests' })
    await user.unhover(itBar)
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()

    // Keyboard users reach the bar after the Chart and Table toggles.
    await user.tab()
    await user.tab()
    await user.tab()
    expect(itBar).toHaveFocus()
    expect(screen.getByRole('tooltip')).toHaveTextContent('6 open')
  })

  it('offers the same numbers as a table', async () => {
    const user = userEvent.setup()
    render(<CategoryChart data={data} />)

    await user.click(screen.getByRole('button', { name: 'Table' }))

    expect(screen.getByRole('table')).toHaveTextContent('Finance00%')
    expect(screen.getByRole('cell', { name: '25%' })).toBeInTheDocument()
  })
})

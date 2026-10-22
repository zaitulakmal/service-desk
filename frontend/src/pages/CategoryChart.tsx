import { useState } from 'react'
import type { TicketCategory } from '../api/types'
import { categoryLabels } from '../lib/labels'

interface CategoryCount {
  category: TicketCategory
  count: number
}

/**
 * One series, so one colour and no legend: the title says what is plotted. Every bar is
 * labelled at its tip, hovering or focusing a bar shows the share, and the same numbers
 * are available as a table.
 */
export function CategoryChart({ data }: { data: CategoryCount[] }) {
  const [view, setView] = useState<'chart' | 'table'>('chart')
  const [hovered, setHovered] = useState<TicketCategory | null>(null)
  const total = data.reduce((sum, d) => sum + d.count, 0)
  const max = Math.max(1, ...data.map((d) => d.count))
  const share = (count: number) => (total ? `${Math.round((count / total) * 100)}%` : '0%')

  return (
    <section className="card chart-card" aria-labelledby="category-heading">
      <div className="card-heading-row">
        <div>
          <h2 id="category-heading" className="card-title">
            Open requests by category
          </h2>
          <span className="muted small">Open and in progress right now</span>
        </div>
        <div className="segmented-tabs small" role="group" aria-label="View">
          <button type="button" aria-pressed={view === 'chart'} onClick={() => setView('chart')}>
            Chart
          </button>
          <button type="button" aria-pressed={view === 'table'} onClick={() => setView('table')}>
            Table
          </button>
        </div>
      </div>

      {view === 'chart' ? (
        <div className="bars">
          {data.map((d) => (
            <button
              key={d.category}
              type="button"
              className="bar-row"
              aria-label={`${categoryLabels[d.category]}: ${d.count} open requests`}
              onMouseEnter={() => setHovered(d.category)}
              onMouseLeave={() => setHovered(null)}
              onFocus={() => setHovered(d.category)}
              onBlur={() => setHovered(null)}
            >
              <span className="bar-label">{categoryLabels[d.category]}</span>
              <span className="bar-track">
                <span className="bar" style={{ width: `${(d.count / max) * 88}%`, opacity: hovered === d.category ? 0.8 : 1 }} />
                <span className="bar-value">{d.count}</span>
              </span>
              {hovered === d.category && (
                <span role="tooltip" className="tooltip">
                  <span className="tooltip-key" aria-hidden="true" />
                  <strong>{d.count} open</strong>
                  <span className="tooltip-label">
                    {categoryLabels[d.category]} · {share(d.count)} of total
                  </span>
                </span>
              )}
            </button>
          ))}
        </div>
      ) : (
        <table className="table compact">
          <thead>
            <tr>
              <th scope="col">Category</th>
              <th scope="col" className="numeric">
                Open
              </th>
              <th scope="col" className="numeric">
                Share
              </th>
            </tr>
          </thead>
          <tbody>
            {data.map((d) => (
              <tr key={d.category}>
                <td>{categoryLabels[d.category]}</td>
                <td className="numeric strong">{d.count}</td>
                <td className="numeric secondary">{share(d.count)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}

import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout'
import { ApprovalsPage } from './pages/ApprovalsPage'
import { DashboardPage } from './pages/DashboardPage'
import { MyRequestsPage } from './pages/MyRequestsPage'
import { NewRequestPage } from './pages/NewRequestPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { QueuePage } from './pages/QueuePage'
import { TicketPage } from './pages/TicketPage'

/**
 * Created only after sign-in, so the router starts on the page the person originally
 * asked for (restored from the login state), not on the login callback URL.
 */
export function createAppRouter() {
  return createBrowserRouter([
    {
      element: <Layout />,
      children: [
        { index: true, element: <MyRequestsPage /> },
        { path: 'new', element: <NewRequestPage /> },
        { path: 'tickets/:id', element: <TicketPage /> },
        { path: 'queue', element: <QueuePage /> },
        { path: 'approvals', element: <ApprovalsPage /> },
        { path: 'dashboard', element: <DashboardPage /> },
        { path: '*', element: <NotFoundPage /> },
      ],
    },
  ])
}

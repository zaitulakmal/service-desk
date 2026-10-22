import { Link } from 'react-router'
import { PageHeader } from '../components/States'

export function NotFoundPage() {
  return (
    <div className="page">
      <PageHeader title="Page not found" subtitle="The page you followed does not exist, or you do not have access to it." />
      <Link to="/" className="button button-primary">
        Go to my requests
      </Link>
    </div>
  )
}

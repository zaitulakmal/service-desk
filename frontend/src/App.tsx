import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { RouterProvider } from 'react-router/dom'
import type { LoginState } from './auth/oidc'
import { createAppRouter } from './router'

function SignedInApp() {
  const [router] = useState(createAppRouter)
  return <RouterProvider router={router} />
}

/**
 * Company apps sign people in straight away with single sign-on, so there is no
 * login page here: anyone without a session is sent to Keycloak and comes back.
 */
export function App() {
  const auth = useAuth()
  const needsLogin = !auth.isLoading && !auth.isAuthenticated && !auth.error && !auth.activeNavigator

  useEffect(() => {
    if (needsLogin) {
      const state: LoginState = { returnTo: window.location.pathname + window.location.search }
      void auth.signinRedirect({ state })
    }
  }, [needsLogin, auth])

  if (auth.error) {
    return (
      <div className="full-page-message">
        <h1>We could not sign you in</h1>
        <p>{auth.error.message}</p>
        <button type="button" className="button button-primary" onClick={() => void auth.signinRedirect()}>
          Try again
        </button>
      </div>
    )
  }
  if (!auth.isAuthenticated) {
    return <div className="full-page-message" aria-live="polite">Signing you in…</div>
  }
  return <SignedInApp />
}

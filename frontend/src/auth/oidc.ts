import { WebStorageStateStore, type User } from 'oidc-client-ts'
import type { AuthProviderProps } from 'react-oidc-context'
import { config } from '../config'

/** Where to go after login, remembered across the round trip to Keycloak. */
export interface LoginState {
  returnTo: string
}

/**
 * Authorization Code flow with PKCE (the library does PKCE by default). The browser
 * never sees a client secret; Keycloak redirects back with a one-time code.
 */
export const oidcConfig: AuthProviderProps = {
  authority: config.oidcAuthority,
  client_id: config.oidcClientId,
  redirect_uri: `${window.location.origin}/`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  scope: 'openid profile email',
  automaticSilentRenew: true,
  // Session storage: tokens are gone when the tab closes.
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  onSigninCallback: (user: User | undefined) => {
    const state = user?.state as LoginState | undefined
    window.history.replaceState({}, document.title, state?.returnTo ?? '/')
  },
}

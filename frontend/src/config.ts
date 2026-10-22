interface RuntimeConfig {
  /** Base URL of the API. Empty string means "same origin" (behind the Kubernetes ingress). */
  apiUrl: string
  oidcAuthority: string
  oidcClientId: string
}

declare global {
  interface Window {
    __SERVICEDESK_CONFIG__?: Partial<RuntimeConfig>
  }
}

const runtime = window.__SERVICEDESK_CONFIG__ ?? {}

export const config: RuntimeConfig = {
  apiUrl: runtime.apiUrl ?? 'http://localhost:8080',
  oidcAuthority: runtime.oidcAuthority ?? 'http://localhost:8180/realms/servicedesk',
  oidcClientId: runtime.oidcClientId ?? 'servicedesk-web',
}

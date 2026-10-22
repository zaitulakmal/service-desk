// Writes dist/config.js from environment variables, so a static host (Vercel) can point
// the same build at any backend. Without the variables, the local-development config stays.
//
//   SERVICEDESK_API_URL         e.g. https://api.203-0-113-5.sslip.io
//   SERVICEDESK_OIDC_AUTHORITY  e.g. https://auth.203-0-113-5.sslip.io/realms/servicedesk
//   SERVICEDESK_OIDC_CLIENT_ID  optional, defaults to servicedesk-web
import { writeFileSync } from 'node:fs'

const apiUrl = process.env.SERVICEDESK_API_URL
const oidcAuthority = process.env.SERVICEDESK_OIDC_AUTHORITY

if (!apiUrl || !oidcAuthority) {
  console.log('write-config: SERVICEDESK_API_URL / SERVICEDESK_OIDC_AUTHORITY not set, keeping dist/config.js')
} else {
  const config = {
    apiUrl: apiUrl.replace(/\/$/, ''),
    oidcAuthority: oidcAuthority.replace(/\/$/, ''),
    oidcClientId: process.env.SERVICEDESK_OIDC_CLIENT_ID ?? 'servicedesk-web',
  }
  writeFileSync('dist/config.js', `window.__SERVICEDESK_CONFIG__ = ${JSON.stringify(config, null, 2)}\n`)
  console.log(`write-config: dist/config.js now points at ${config.apiUrl}`)
}

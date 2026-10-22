// Settings for local development.
// Kubernetes mounts its own copy of this file (see the Helm chart), so the same image works everywhere.
window.__SERVICEDESK_CONFIG__ = {
  apiUrl: 'http://localhost:8080',
  oidcAuthority: 'http://localhost:8180/realms/servicedesk',
  oidcClientId: 'servicedesk-web',
}

# AGENTS.md — service-desk

Project contract. `CLAUDE.md` is a symlink to this file.

## Purpose
An enterprise service desk (internal ticketing + approvals, like ServiceNow /
Jira Service Management): staff raise requests to IT, HR, Finance or
Facilities; agents work a queue with SLAs; multi-step approvals; a manager
dashboard; email notifications.

Zaitul's portfolio project for moving into MNC software engineering roles. It
deliberately exercises what MNC job descriptions ask for, but every technology
must solve a real problem in the system (interviewers ask "why Kafka here?").

- **Claude writes the code** (Zaitul's decision, 2026-10-20) and explains each
  part so Zaitul can defend it in interviews.
- Scope (Zaitul, 2026-10-21): **everything in the roadmap**, real SSO login now,
  **no AI features for now** (needs a paid API key; add later).
- UI must look like the approved mockup:
  https://claude.ai/artifact/KFJhuFxP6fgh653rT9zEKB

## Layout
```
api/                   Spring Boot API (tickets, approvals, SLA, attachments, dashboard)
notification-service/  Spring Boot Kafka consumer that sends emails
frontend/              React + TypeScript + Vite web app
deploy/helm/           Helm chart (apps + per-component in-cluster infra); realm JSON in files/
deploy/monitoring/     Prometheus + Grafana values and the dashboard JSON
deploy/terraform/aws/  AWS: VPC, EKS Auto Mode, RDS, S3, ECR, IAM (Pod Identity, GitHub OIDC)
deploy/aws/            one-time EKS setup (IngressClass alb, gp3 StorageClass)
deploy/postgres/       init.sql for docker compose
.github/workflows/     ci.yml (tests, e2e, images to GHCR), deploy-aws.yml (manual)
docs/screenshots/      README screenshots
compose.yaml           local infrastructure for development
```

## Stack (decided)
- Java 21, Spring Boot 4.1.1, Maven wrapper per service
- PostgreSQL 16 + Flyway (one database per service); Hibernate `ddl-auto=validate`
- Keycloak 26.8.1 (realm `servicedesk`), API is an OAuth2 resource server (JWT,
  issuer + audience `servicedesk-api`), roles from `realm_access.roles`
- Kafka 4.3.1 (KRaft), topic `servicedesk.ticket-events`, transactional outbox in the
  API, idempotent consumer + retry + `.DLT` dead-letter topic in notification-service
- Attachments: S3 API via AWS SDK v2; RustFS 1.0.3 locally/in-cluster (MinIO images
  are no longer published), real S3 on AWS
- Mailpit for email in local/dev
- Frontend: React 19, Vite 8, TypeScript 6, react-router 8, TanStack Query 5,
  oidc-client-ts + react-oidc-context (Auth Code + PKCE), plain CSS, IBM Plex fonts
  via @fontsource; Vitest + Testing Library; Playwright e2e
- Deploy: Docker multi-stage images (non-root uid 10001, layered jars; nginx-unprivileged
  for the web app), Helm chart with HPA (2–4 API pods), PDB, probes, read-only root FS.
  Local cluster: Rancher Desktop k3s + Traefik at http://servicedesk.localhost
  (auth./mail./grafana.servicedesk.localhost). AWS: values-aws.yaml (RDS, S3, ALB).
- Monitoring: prometheus-community/prometheus 29.37.1, grafana-community/grafana 14.0.1
  (the old grafana/grafana repo stopped updating). Grafana 13 needs ~1 GiB memory.
  Business metrics in `ServiceDeskMetrics`; counters are pre-registered at 0. Do not end
  a counter name with `.created` — Prometheus reserves `_created` (it gets dropped).
- CI: GitHub Actions (checkout v7, setup-java v6, setup-node v7, docker actions v4/v7);
  validated with actionlint. Terraform 1.17, aws provider ~> 6.69, vpc ~> 6.7, eks ~> 21.29;
  `terraform validate` passes. Nothing has been applied to AWS.

## Domain rules
- Ticket statuses: AWAITING_APPROVAL, OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED.
  Manual (agent) moves: OPEN→IN_PROGRESS|CLOSED, IN_PROGRESS→RESOLVED,
  RESOLVED→CLOSED|IN_PROGRESS. Assigning an OPEN ticket moves it to IN_PROGRESS.
- Types: INCIDENT, SERVICE_REQUEST, ACCESS, PURCHASE.
  Approvals: ACCESS → manager then IT Security (role `security-approver`);
  PURCHASE > RM 1,000 → manager. No manager on record → manager step skipped.
  Nobody approves their own request. Rejecting needs a reason.
- SLA (calendar time, resolution target): URGENT 4h, HIGH 8h, MEDIUM 3d, LOW 5d.
  Paused while AWAITING_APPROVAL; AT_RISK under 20% left; breach job escalates to
  `service-manager` users once.
- Roles: `staff`, `agent` (works tickets of their `team` = category),
  `security-approver`, `service-manager` (dashboard).
- Visibility: requester, assignee, team agents, service managers, people in the
  approval chain. Others get 404.

## Conventions
- All code, comments, README, commit messages, PR descriptions in English.
- Java: tabs, `this.` on fields, constructor injection, records for DTOs, package by feature.
- Every endpoint/service ships with tests. Schema changes only via new Flyway migrations.

## Run locally
```bash
docker compose up -d                                  # postgres, keycloak, kafka, mailpit, rustfs
(cd api && ./mvnw spring-boot:run)                    # :8080
(cd notification-service && ./mvnw spring-boot:run)   # :8081
(cd frontend && npm install && npm run dev)           # :5173 (needs Node >= 22.12)
```
Demo logins (password `password`): aina (staff), farah (manager), daniel (IT agent +
IT Security approver), mei (HR agent), siti (service manager), hafiz, kumar.

Kubernetes (local): `helm upgrade --install service-desk deploy/helm/service-desk -n service-desk --create-namespace`
after building the images (see README). e2e against it: `E2E_BASE_URL=http://servicedesk.localhost npm run e2e`.

Machine notes (Zaitul's Mac): Rancher Desktop VM set to 8 GB / 4 CPU (2026-10-21).
Default `node` in the shell is 22.5.1 (/usr/local/bin); use Homebrew Node 26
(`PATH=/opt/homebrew/bin:$PATH`) for the frontend. Testcontainers needs
`~/.testcontainers.properties` (docker.host = Rancher socket) and
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` (in `~/.zshrc`).

## Roadmap / status
1. Ticket module ✅
2. SSO (Keycloak) + roles ✅
3. Queue, SLA + escalation, approvals, comments, activity log, dashboard ✅
4. Kafka outbox + notification-service (email, idempotency, DLT) ✅
5. Attachments on S3-compatible storage ✅
6. React frontend matching the mockup ✅ (unit + Playwright e2e)
7. Docker images + Helm chart on local Kubernetes ✅ (e2e green against the cluster)
8. Observability: Prometheus + Grafana ✅
9. CI/CD (GitHub Actions) + Terraform for AWS ✅ written and validated; not run (no repo / AWS account yet)
10. Later: AI features (needs API key), CSP header, BFF, Alertmanager rules

## Open questions
- GitHub repo: not created, nothing committed yet. Ask Zaitul before creating
  the repo, committing or pushing.
- AWS: applying Terraform needs Zaitul's AWS account and costs money. Ask first.
